angular.module("wust.components").controller("FocusCtrl", FocusCtrl);

FocusCtrl.$inject = ["Helpers", "Post", "$stateParams", "$state", "HistoryService", "ConnectedComponents", "$q", "$scope", "LiveService", "Session"];

function FocusCtrl(Helpers, Post, $stateParams, $state, HistoryService, ConnectedComponents, $q, $scope, LiveService, Session) {
    let vm = this;

    if ($stateParams.id === "") {
        $state.go("dashboard");
    } else {
        let rootNodePromise = Post.$find($stateParams.id).$then(rootNode => {
            vm.rootNodeLoaded = true;
            let rawRootNode = rootNode.encode();
            let graph = {
                nodes: [rawRootNode],
                relations: [],
                $pk: rootNode.id
            };
            vm.component = renesca.js.GraphFactory().fromRecord(graph);
            vm.graphComponent = vm.component.wrap("graph");
            vm.neighboursComponent = vm.component.hyperWrap("neighbours");
            vm.componentLoading = true;

            // we are viewing details about a node, so add it to the nodehistory
            HistoryService.add(vm.graphComponent.rootNode);
            HistoryService.setCurrentViewComponent(vm.component);

        }, () => $state.go("dashboard"));

        ConnectedComponents.$find($stateParams.id).$then(response => {
            rootNodePromise.$then(() => {
                vm.componentLoading = false;
                response.nodes.forEach(n => vm.graphComponent.addNode(n));
                response.relations.forEach(r => vm.graphComponent.addRelation(r));
                vm.graphComponent.commit();

                LiveService.registerNodes(vm.graphComponent.nodes);

                if (vm.tabViews[2].active && vm.component.urlResources.length === 0) {
                    vm.tabViews[2]._active = false;
                    vm.tabViews[0].active = true;
                }

                //TODO: better way?
                if (_.any([vm.graphComponent.rootNode].concat(vm.graphComponent.rootNode.deepPredecessors), n => _.any(Session.notifications, _.pick(n, "id")))) {
                    //TODO: calculate locally?
                    Session.loadNotifications();
                }
            });
        }, () => vm.componentLoading = false);
    }

    class Tab {
        constructor(index) {
            this.index = index;
        }
        get active() {
            return this._active;
        }
        set active(active) {
            // we fire a resize event graph whenever the graph becomes active
            // we use our knowledge to know that index = 1 is the graph
            if (active) {
                if (this.index === 1) {
                    $state.transitionTo("focus", { type: "graph" }, { inherit: true, notify: false });
                    // switching to graph by tabs
                    // console.log("focusCtrl: sending display_graph");
                    $scope.$broadcast("display_graph");
                } else if (this.index === 2) {
                    $state.transitionTo("focus", { type: "links" }, { inherit: true, notify: false });
                } else {
                    $state.transitionTo("focus", { type: "" }, { inherit: true, notify: false });
                }
            }

            this._active = active;
        }
    }

    vm.tabViews = _.map([0, 1, 2], i => new Tab(i));
    $scope.$on("focus.neighbours", () => vm.tabViews[0].active = true);
    $scope.$on("$destroy", () => {
        HistoryService.setCurrentViewComponent(undefined);
    });

    setTabs();

    function setTabs() {
        if ($stateParams.type === "graph") {
            // opening graph by url
            vm.tabViews[0]._active = false;
            vm.tabViews[1]._active = true;
            vm.tabViews[2]._active = false;
        } else if ($stateParams.type === "links") {
            // opening graph by url
            vm.tabViews[0]._active = false;
            vm.tabViews[1]._active = false;
            vm.tabViews[2]._active = true;
        } else {
            vm.tabViews[0]._active = true;
            vm.tabViews[1]._active = false;
            vm.tabViews[2]._active = false;
        }
    }
}
