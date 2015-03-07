app.controller('GraphsCtrl', function($scope, $state, $filter, Graph, initialData) {
    $scope.$watch('search.label', filter);
    $scope.search = {
        label: ""
    };

    var colorMappings = {
        GOAL: "#00DDDD",
        PROBLEM: "#DD8047",
        IDEA: "#005CA3"
    };

    initialData.nodes = initialData.nodes.map(function(node) {
        return {
            id: node.id,
            label: node.title === "" ? "" : node.label + ": " + node.title,
            color: colorMappings[node.label]
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
            nodes: {
                shape: 'box',
            },
            edges: {
                style: 'arrow'
            },
        }
    };

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
