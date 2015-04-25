angular.module("wust").controller("GraphsCtrl", function($scope, $state, $filter, Graph, DiscourseNode) {
    $scope.onClick = onClick;
    $scope.search = {
        title: ""
    };

    $scope.graph = Graph.$fetch().$then(create);

    $scope.$watch("search.title", filter);

    function filter() {
        let filtered = $filter("fuzzyFilter")($scope.graph.nodes, $scope.search);
        $scope.$broadcast("d3graph_filter", filtered);
    }

    function create() {
        $scope.$broadcast("d3graph_redraw");
    }

    function onClick(d) {
        let state = DiscourseNode.get(d.label).state;
        $state.go(state, {
            id: d.id
        });
    }
});
