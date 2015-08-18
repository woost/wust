angular.module("wust.components").controller("FocusCtrl", FocusCtrl);

FocusCtrl.$inject = ["Helpers", "$stateParams", "HistoryService", "rootNode", "ConnectedComponents", "$q"];

function FocusCtrl(Helpers, $stateParams, HistoryService, rootNode, ConnectedComponents, $q) {
    let vm = this;

    class Tab {
        constructor(index) {
            this.index = index;
        }
        get active() {
            return this._active;
        }
        set active(active) {
            this._active = active;
            // if (active)
            //     HistoryService.changeActiveView(this.index);

            // fire window.resize event to recalculate d3 graph node dimensions.
            // they are 0x0 in some cases.
            setTimeout( () => Helpers.fireWindowResizeEvent(), 200 );
        }
    }

    let rawRootNode = rootNode.$encode();
    //TODO: encode misses tags
    rawRootNode.tags = rootNode.tags.$encode();

    let graph = {
        nodes: [rawRootNode],
        relations: [],
        $pk: rootNode.id
    };
    let component = renesca.js.GraphFactory().fromRecord(graph);
    vm.graphComponent = component.wrap("graph");
    vm.neighboursComponent = component.hyperWrap("neighbours");
    vm.componentLoading = true;

    vm.tabViews = _.map([0, 1, 2, 3], i => new Tab(i));
    // vm.tabViews[HistoryService.activeViewIndex]._active = true;
    vm.tabViews[0]._active = true;

    // we are viewing details about a node, so add it to the nodehistory
    HistoryService.add(vm.graphComponent.rootNode);
    HistoryService.currentViewComponent = component;

    // keep tags sorted by weight
    sortTagsOnGraph(vm.graphComponent);
    vm.graphComponent.onCommit(() => sortTagsOnGraph(vm.graphComponent));

    ConnectedComponents.$find($stateParams.id).$then(response => {
        vm.componentLoading = false;
        response.nodes.forEach(n => vm.graphComponent.addNode(n));
        response.relations.forEach(r => vm.graphComponent.addRelation(r));
        vm.graphComponent.commit();
    }, () => vm.componentLoading = false);

    function sortTagsOnGraph(graph) {
        graph.nodes.forEach(n => n.tags = Helpers.sortTags(n.tags));
    }
}
