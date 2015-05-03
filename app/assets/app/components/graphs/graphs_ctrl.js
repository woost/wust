angular.module("wust.components").controller("GraphsCtrl", function($scope, $filter, Graph, DiscourseNode) {
    $scope.onClick = onClick;
    $scope.search = {
        title: ""
    };

    $scope.graph = {};
    Graph.$fetch().$then(createGraph);

    $scope.$watch("search.title", filter);

    function createGraph(graph) {
        graph.nodes = graph.nodes.map(node => _.merge(node, {
            css: "node " + DiscourseNode.get(node.label).css
        }));
        $scope.graph = graph;
    }

    function filter() {
        let filtered = $filter("fuzzyFilter")($scope.graph.nodes, $scope.search);
        $scope.$broadcast("d3graph_filter", filtered);
    }

    function onClick(d) {
        DiscourseNode.get(d.label).gotoState(d.id);
    }
});
