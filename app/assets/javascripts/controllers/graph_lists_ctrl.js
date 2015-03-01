app.controller('GraphListsCtrl', function($scope, $state, $filter, initialData) {
    $scope.$watch('search.label', filter);

    var nodes = new vis.DataSet();
    var edges = new vis.DataSet();
    nodes.add(initialData.nodes);
    edges.add(initialData.edges);
    $scope.data = {
        graph: {
            nodes: nodes,
            edges: edges
        },
        options: {
            navigation: true,
            dataManipulation: true,
            nodes: {
                shape: 'box',
                mass: 1.2
            },
            edges: {
                style: 'arrow'
            },
        },
        onSelect: onSelect
    };

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
        nodes.update(filtered);
        nodes.forEach(function(node) {
            if (!filtered.find(function(n) { return n.id === node.id; })) {
                nodes.remove(node.id);
            }
        });
    }
});
