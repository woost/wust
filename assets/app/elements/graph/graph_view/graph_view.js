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

graphViewCtrl.$inject = ["$scope", "$rootScope", "$stateParams", "$filter", "EditService", "$state", "ConnectedComponents"];

function graphViewCtrl($scope, $rootScope, $stateParams, $filter, EditService, $state, ConnectedComponents) {
    let vm = this;

    vm.isConverged = false;
    vm.addNodeToGraph = addNodeToGraph;
    vm.focusNode = focusNode;
    vm.onDraw = () => $scope.$apply(() => vm.isConverged = true);
    vm.filter = filter;
    vm.search = {
        title: ""
    };

    $scope.$on("component.changed", () => {
        vm.tagSuggestions = angular.copy(_.uniq(_.flatten(vm.graph.nodes.map(n => n.tags.concat(n.classifications))), "id"));
        $scope.$broadcast("tageditor.suggestions");
    });

    function filter() {
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
        ConnectedComponents.$find(node.id, {depth: 1}).$then(result => {
            result.nodes.forEach(node => vm.graph.addNode(node));
            result.relations.forEach(relation => vm.graph.addRelation(relation));
            vm.graph.commit();

            let wrappedNode = vm.graph.nodeById(node.id);
            vm.d3Info.positionNode(wrappedNode, event.offsetX, event.offsetY);
        });
    }

    function focusNode(node) {
        //TODO: duplicate controller instanciation with go
        if ($stateParams.id === node.id)
            $rootScope.$broadcast("focus.neighbours");
        else
            $state.go("focus", { id: node.id, type: "" });
    }
}
