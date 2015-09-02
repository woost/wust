angular.module("wust.elements").directive("graphView", graphView);

graphView.$inject = [];

function graphView() {
    return {
        restrict: "A",
        templateUrl: "elements/graph/graph_view/graph_view.html",
        replace: true,
        scope: {
            graph: "=",
            isLoading: "="
        },
        controller: graphViewCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

graphViewCtrl.$inject = ["$scope", "$stateParams", "$filter", "EditService", "$state"];

function graphViewCtrl($scope, $stateParams, $filter, EditService, $state) {
    let vm = this;

    vm.isConverged = false;
    vm.addNodeToGraph = addNodeToGraph;
    vm.onClick = onClick;
    vm.onDraw = () => vm.isConverged = true;
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
        vm.d3Info.filter(matchingNodes);
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

        vm.d3Info.positionNode(node, event.offsetX, event.offsetY);
    }

    function onClick(node) {
        $state.go("focus", { id: node.id, type: "" });
    }
}
