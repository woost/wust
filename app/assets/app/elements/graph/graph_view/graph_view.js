angular.module("wust.elements").directive("graphView", graphView);

graphView.$inject = [];

function graphView() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/graph/graph_view/graph_view.html",
        replace: true,
        scope: {
            graph: "="
        },
        controller: graphViewCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

graphViewCtrl.$inject = ["$scope", "DiscourseNode", "$filter", "EditService", "$state"];

function graphViewCtrl($scope, DiscourseNode, $filter, EditService, $state) {
    let vm = this;

    vm.addNodeToGraph = addNodeToGraph;
    vm.state = {};
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

        let matchingNodes = $filter("fuzzyFilter")(_.reject(vm.graph.nodes, { isHyperRelation: true }), vm.search);
        vm.d3Graph.filter(matchingNodes);
    }

    function addNodeToGraph(node, event) {
        // we can drop local nodes from the scratchpad here,
        // so let us create them if before putting them in the graph, so we
        // have an id in the graph
        if (node.id === undefined) {
            EditService.findNode(node.localId).save().$then(data => placeNodeInGraph(data, event));
        } else {
            placeNodeInGraph(node, event);
        }
    }

    function placeNodeInGraph(node, event) {
        vm.graph.addNode(node);
        vm.graph.commit();
        let wrappedNode = vm.graph.nodeById(node.id);

        vm.d3Graph.setNodePositionFromOffset(wrappedNode, event.offsetX, event.offsetY);
        vm.d3Graph.setFixed(wrappedNode);
        vm.d3Graph.drawGraph();
    }

    function onClick(node) {
        $state.go("focus", _.pick(node, "id"));
    }
}
