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

    vm.graphLoading = true;
    vm.onDraw = onDraw;
    vm.onClick = onClick;
    vm.search = {
        title: ""
    };

    vm.graph.$then(createGraph);

    $scope.$watch("vm.search.title", filter);

    function createGraph(graph) {
        graph.nodes = graph.nodes.map(node => _.merge(node, {
            css: node.hyperEdge ? "relation_label" : `node ${DiscourseNode.get(node.label).css}`
        }));
    }

    function filter() {
        let filtered = $filter("fuzzyFilter")(_.reject(vm.graph.nodes, { hyperEdge: true }), vm.search);
        $scope.$broadcast("d3graph_filter", filtered);
    }

    function onClick(node) {
        DiscourseNode.get(node.label).gotoState(node.id);
    }

    function onDraw() {
        $scope.$apply(() => vm.graphLoading = false);
    }
}
