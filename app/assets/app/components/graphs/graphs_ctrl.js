angular.module("wust.components").controller("GraphsCtrl", GraphsCtrl);

GraphsCtrl.$inject = ["$scope", "$filter", "Graph", "DiscourseNode"];

function GraphsCtrl($scope, $filter, Graph, DiscourseNode) {
    $scope.onClick = onClick;
    $scope.search = {
        title: ""
    };

    $scope.graph = {};
    Graph.$fetch().$then(createGraph);

    $scope.$watch("search.title", filter);

    function createGraph(graph) {
        graph.nodes = graph.nodes.map(node => _.merge(node, {
            css: node.hyperEdge ? "" : `node ${DiscourseNode.get(node.label).css}`
        }));
        $scope.graph = graph;
    }

    function filter() {
        let filtered = $filter("fuzzyFilter")(_.filter($scope.graph.nodes, { hyperEdge: false}), $scope.search);
        $scope.$broadcast("d3graph_filter", filtered);
    }

    function onClick(d) {
        DiscourseNode.get(d.label).gotoState(d.id);
    }
}
