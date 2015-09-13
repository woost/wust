angular.module("wust.components").controller("FocusCtrl", FocusCtrl);

FocusCtrl.$inject = ["Helpers", "$stateParams", "$state", "HistoryService", "rootNode", "ConnectedComponents", "$q", "$scope"];

function FocusCtrl(Helpers, $stateParams, $state, HistoryService, rootNode, ConnectedComponents, $q, $scope) {
    let vm = this;

    let rawRootNode = rootNode.encode();

    let graph = {
        nodes: [rawRootNode],
        relations: [],
        $pk: rootNode.id
    };
    let component = renesca.js.GraphFactory().fromRecord(graph);
    vm.graphComponent = component.wrap("graph");
    vm.neighboursComponent = component.hyperWrap("neighbours");
    vm.componentLoading = true;

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
                } else {
                    $state.transitionTo("focus", { type: "" }, { inherit: true, notify: false });
                }
            }

            this._active = active;
        }
    }

    vm.tabViews = _.map([0, 1], i => new Tab(i));
    if ($stateParams.type === "graph") {
        // opening graph by url
        vm.tabViews[0]._active = false;
        vm.tabViews[1]._active = true;
    } else {
        vm.tabViews[0]._active = true;
        vm.tabViews[1]._active = false;
    }

    // we are viewing details about a node, so add it to the nodehistory
    HistoryService.add(vm.graphComponent.rootNode);
    HistoryService.currentViewComponent = component;

    ConnectedComponents.$find($stateParams.id).$then(response => {
        vm.componentLoading = false;
        response.nodes.forEach(n => vm.graphComponent.addNode(n));
        response.relations.forEach(r => vm.graphComponent.addRelation(r));
        vm.graphComponent.commit();
    }, () => vm.componentLoading = false);
}
