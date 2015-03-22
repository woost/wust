angular.module("wust").controller("GraphsCtrl", function($scope, $state, $filter, Graph, DiscourseNode) {
    Graph.get().$promise.then(createGraph);

    $scope.onClick = onClick;
    $scope.graph = {
        nodes: [],
        edges: []
    };

    $scope.search = {
        title: ""
    };

    function filter(graph) {
        var edgeMap = _(graph.edges).map(edge => {
            //todo: helper function to construct object with var on lhs
            var res = {};
            res[edge.from] = [edge.to];
            res[edge.to] = [edge.from];
            return res;
        }).reduce(_.partialRight(_.merge, (a, b) => {
            return a ? a.concat(b) : b;
        }, _));

        return function() {
            var filtered = $filter("fuzzyFilter")(graph.nodes, $scope.search);
            var ids = _.map(filtered, "id");
            for (let i = 0; i < ids.length; i++) {
                ids = _.union(ids, edgeMap[ids[i]]);
            }

            $scope.graph.nodes = _.map(graph.nodes, node => {
                var visible = _(ids).includes(node.id);
                return _.merge(node, {visible: visible});
            });
            $scope.graph.edges = _.map(graph.edges, edge => {
                var visible = _(ids).includes(edge.from) && _(ids).includes(edge.to);
                return _.merge(edge, {visible: visible});
            });
        };
    }

    function createGraph(graph) {
        // TODO: not efficient
        // we need to reference nodes via their index in the nodes array, because d3 is weird.
        graph.edges = graph.edges.map(edge => {
            return _.merge(edge, {
                source: _.findIndex(graph.nodes, {
                    id: edge.from
                }),
                target: _.findIndex(graph.nodes, {
                    id: edge.to
                }),
                label: edge.label.toLowerCase(),
                // TODO: calculate real connection strength
                strength: _.random(1, 5)
            });
        });

        $scope.graph.nodes = graph.nodes;
        $scope.graph.edges = graph.edges;
        $scope.$watch("search.title", filter(graph));
    }

    function onClick(d) {
        var state = DiscourseNode.get(d.label).state;
        $state.go(state, {
            id: d.id
        });
    }
});
