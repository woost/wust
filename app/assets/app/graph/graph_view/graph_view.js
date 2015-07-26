angular.module("wust.graph").directive("graphView", graphView);

graphView.$inject = [];

function graphView() {
    return {
        restrict: "A",
        templateUrl: "assets/app/graph/graph_view/graph_view.html",
        replace: true,
        scope: {
            graph: "="
        },
        controller: graphViewCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

graphViewCtrl.$inject = ["$scope", "DiscourseNode", "$filter"];

function graphViewCtrl($scope, DiscourseNode, $filter) {
    let vm = this;

    vm.addNodeToGraph = addNodeToGraph;
    vm.onClick = onClick;
    vm.search = {
        title: ""
    };

    let firstFilter = true;
    $scope.$watch("vm.search.title", filter);

    function filter() {
        if (firstFilter) {
            firstFilter = false;
            return;
        }

        let matchingNodes = $filter("fuzzyFilter")(_.reject(vm.graph.nodes, { hyperEdge: true }), vm.search);
        vm.graph.d3Graph.filter(matchingNodes);
    }

    function addNodeToGraph(node, event) {
        vm.graph.addNode(node);
        vm.graph.commit();
        let wrappedNode = vm.graph.nodeById(node.id);

        vm.graph.d3Graph.setNodePositionFromOffset(wrappedNode, event.offsetX, event.offsetY);
        vm.graph.d3Graph.setFixed(wrappedNode);
        vm.graph.d3Graph.drawGraph();
    }

    function onClick(node) {
        DiscourseNode.get(node.label).gotoState(node.id);
    }
}
