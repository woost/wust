app.controller('GraphsCtrl', function($scope, $state, $filter, Graph) {
    var colorMappings = {
        GOAL: "#6DFFB4",
        PROBLEM: "#FFDC6D",
        IDEA: "#6DB5FF",
        PROARGUMENT: "#79FF6D",
        CONARGUMENT: "#FF6D6F"
    };

    var graph = Graph.get().$promise.then(createGraph);
    var nodes = new vis.DataSet();
    var edges = new vis.DataSet();

    $scope.data = {
        graph: {
            nodes: nodes,
            edges: edges
        },
        options: {
            nodes: {
                shape: 'box',
                radius: 4
            },
            edges: {
                color: '#353535',
                style: 'arrow'
            },
        }
    };

    $scope.$watch('search.label', filter);
    $scope.search = {
        label: ""
    };

    function filter() {
        var filtered = $filter('filter')(graph.nodes, $scope.search);
        nodes.update(filtered);
        angular.forEach(nodes, function(node) {
            if ($filter('filter')(filtered, {
                id: node.id
            }).length === 0) {
                nodes.remove(node.id);
            }
        });
    }

    function createGraph(graph) {
        graph.nodes = graph.nodes.map(function(node) {
            return {
                id: node.id,
                label: node.title,
                color: colorMappings[node.label]
            };
        });

        nodes.add(graph.nodes);
        edges.add(graph.edges);
    }
});
