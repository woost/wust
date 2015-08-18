angular.module("wust.components").controller("FocusCtrl", FocusCtrl);

FocusCtrl.$inject = ["Helpers", "FocusService", "$stateParams", "HistoryService", "rootNode", "ConnectedComponents", "$q"];

function FocusCtrl(Helpers, FocusService, $stateParams, HistoryService, rootNode, ConnectedComponents, $q) {
    let vm = this;

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

    vm.tabViews = FocusService.tabViews;
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
