angular.module("wust.elements").directive("graphView", graphView);

graphView.$inject = [];

function graphView() {
    return {
        restrict: "A",
        templateUrl: "elements/graph/graph_view/graph_view.html",
        replace: true,
        scope: {
            graph: "="
        },
        controller: graphViewCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

graphViewCtrl.$inject = ["$scope", "$stateParams", "FocusService", "$filter", "EditService", "$state"];

function graphViewCtrl($scope, $stateParams, FocusService, $filter, EditService, $state) {
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
        // we can drop local nodes from the scratchpad here.
        // so we check whether the node was edited and has unsaved changes and
        // save it before adding it to the graph.
        let editedNode = EditService.findNode(node.localId);
        if (editedNode !== undefined && editedNode.canSave) {
            editedNode.save().$then(data => placeNodeInGraph(data, event));
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
        if (node.id === $stateParams.id)
            FocusService.activateTab(0); // show neighbours view
        else
            $state.go("focus", { id: node.id, type: undefined });
    }
}
