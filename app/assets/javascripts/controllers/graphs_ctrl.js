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
        var nodeMap = _(graph.nodes).map(function(node) {
            var res = {};
            res[node.id] = node;
            return res;
        }).reduce(_.merge);

        var edgeMap = _(graph.edges).map(function(edge) {
            var res = {};
            res[edge.from] = [edge.to];
            res[edge.to] = [edge.from];
            return res;
        }).reduce(_.partialRight(_.merge, function(a, b) {
            return a ? a.concat(b) : b;
        }, _));

        return function() {
            var filtered = $filter('fuzzyFilter')(graph.nodes, $scope.search);
            var ids = _.map(filtered, "id");
            for (var i = 0; i < ids.length; i++) {
                ids = _.union(ids, edgeMap[ids[i]]);
            }

            nodes.remove(_.difference(nodes.getIds(), ids));
            nodes.update(_.map(ids, _.propertyOf(nodeMap)));
        };
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

        $scope.$watch('search.label', filter(graph));
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
