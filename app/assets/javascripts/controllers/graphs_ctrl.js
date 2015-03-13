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
        var filtered = $filter('filter')(graph.nodes, $scope.search);
        nodes.update(filtered);
        nodes.remove(_.difference(nodes.getIds(), _.map(filtered, "id")));
    }

    function createGraph(graph) {
        graph.nodes = graph.nodes.map(function(node) {
            return {
                id: node.id,
                label: node.title,
                origLabel: node.label,
                color: DiscourseNode.get(node.label).color
            };
        });

        nodes.add(graph.nodes);
        edges.add(graph.edges);

        $scope.$watch('search.label', _.wrap(graph, filter));
    }

    function onClick(selected) {
        if (!_.any(selected.nodes))
            return;

        var id = selected.nodes[0];
        var node = nodes.get(id);
        var state = DiscourseNode.get(node.origLabel).state;
        $state.go(state, {
            id: id
        });
    }
});
