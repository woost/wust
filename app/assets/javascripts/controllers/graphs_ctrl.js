app.controller('GraphsCtrl', function($scope, $state, $filter, Graph, DiscourseNode) {
    Graph.get().$promise.then(createGraph);
    $scope.data = {
        graph: {
            nodes: [],
            edges: []
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
        title: ""
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

            $scope.data.graph.nodes = _.map(ids, _.propertyOf(nodeMap));
            $scope.data.graph.edges = _.select(graph.edges, function(edge) {
                return _(ids).includes(edge.from) && _(ids).includes(edge.to);
            });
        };
    }

    function createGraph(graph) {
        graph.edges = graph.edges.map(function(edge) {
            return _.defaults(edge, {
                source: _.findIndex(graph.nodes, {
                    id: edge.from
                }),
                target: _.findIndex(graph.nodes, {
                    id: edge.to
                }),
                strength: 1
            });
        });

        $scope.data.graph.nodes = graph.nodes;
        $scope.data.graph.edges = graph.edges;
        $scope.$watch('search.title', filter(graph));
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
