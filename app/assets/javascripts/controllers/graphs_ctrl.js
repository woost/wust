app.controller('GraphsCtrl', function($scope, $state, $filter, Graph, initialData) {
    $scope.$watch('search.label', filter);
    $scope.search = {
        label: ""
    };

    initialData.nodes = initialData.nodes.map(function(node) {
        return {
            id: node.uuid,
            label: node.label + ": " + node.title,
            title: node.title
        };
    });

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
            dataManipulation: false,
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
        angular.forEach(nodes, function(node) {
            if ($filter('filter')(filtered, { id: node.id }).length === 0) {
                nodes.remove(node.id);
            }
        });
    }
});
