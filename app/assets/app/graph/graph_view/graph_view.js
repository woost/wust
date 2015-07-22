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
        vm.controlGraph.filter(matchingNodes);
    }

    function addNodeToGraph(node, event) {
        console.log(vm.controlGraph);
        vm.graph.addNode(node);
        vm.graph.commit();
        let wrappedNode = vm.graph.nodeById(node.id);
        wrappedNode.x = event.offsetX;
        wrappedNode.y = event.offsetY;
        wrappedNode.fixed = true;
    }

    function onClick(node) {
        DiscourseNode.get(node.label).gotoState(node.id);
    }
}
