app.controller('GraphsCtrl', function($scope, $state, $filter, Graph, DiscourseNode) {
    Graph.get().$promise.then(createGraph);
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
                radius: 4,
                iconFontFace: 'FontAwesome',
                icon: "\uf0c3"
            },
            edges: {
                color: '#353535',
                style: 'arrow'
            },
        },
        onClick: onClick
    };

    $scope.search = {
        label: ""
    };

    function filter(graph) {
        return function() {
            var filtered = $filter('filter')(graph.nodes, $scope.search);
            nodes.update(filtered);
            angular.forEach(nodes, function(node) {
                if ($filter('filter')(filtered, {
                    id: node.id
                }).length === 0) {
                    nodes.remove(node.id);
                }
            });
        };
    }

    function createGraph(graph) {
        graph.nodes = graph.nodes.map(function(node) {
            return {
                id: node.id,
                label: node.title,
                origLabel: node.label,
                color: DiscourseNode.getColor(node.label)
            };
        });

        nodes.add(graph.nodes);
        edges.add(graph.edges);

        $scope.$watch('search.label', filter(graph));
    }

    function onClick(selected) {
        var id = selected.nodes[0];
        var node = nodes.get(id);
        var state = DiscourseNode.getState(node.origLabel);
        $state.go(state, {
            id: id
        });
    }
});
