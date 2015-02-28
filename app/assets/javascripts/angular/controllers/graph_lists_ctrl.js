app.controller('GraphListsCtrl', function($scope, $state, $filter, initialData) {
    var nodes = new vis.DataSet();
    var edges = new vis.DataSet();
    nodes.add(initialData.nodes);
    edges.add(initialData.edges);
    $scope.graph = {
        nodes: nodes,
        edges: edges
    };

    $scope.onSelect = onSelect;
    $scope.$watch('search.label', filter);

    function onSelect(properties) {
        var id = properties.nodes[0];
        if (id === undefined) {
            return;
        }

        $state.go('graphs.detail', {
            id: id
        });
    }

    function filter() {
        var filtered = $filter('filter')(initialData.nodes, $scope.search);
        nodes.clear();
        nodes.add(filtered);
    }
});
