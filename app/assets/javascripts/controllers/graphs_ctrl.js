angular.module("wust").controller("GraphsCtrl", function($scope, $state, $filter, Graph, DiscourseNode) {
    Graph.$fetch().$then(createGraph);

    $scope.onClick = onClick;
    $scope.graph = {
        nodes: [],
        edges: []
    };

    $scope.search = {
        title: ""
    };

    function filter(graph) {
        let edgeMap = _(graph.edges).map(edge => {
            return {
                [edge.from]: [edge.to],
                [edge.to]: [edge.from]
            };
        }).reduce(_.partialRight(_.merge, (a, b) => {
            return a ? a.concat(b) : b;
        }, _));

        return function() {
            let filtered = $filter("fuzzyFilter")(graph.nodes, $scope.search);
            let filteredIds = _.map(filtered, "id");
            let ids = filteredIds;
            for (let i = 0; i < ids.length; i++) {
                ids = _.union(ids, edgeMap[ids[i]]);
            }

            $scope.graph.nodes = _.map(graph.nodes, node => {
                let marked = _(filteredIds).includes(node.id);
                let visible = marked || _(ids).includes(node.id);
                return _.merge(node, {
                    visible: visible,
                    marked: marked
                });
            });
            $scope.graph.edges = _.map(graph.edges, edge => {
                let visible = _(ids).includes(edge.from) && _(ids).includes(edge.to);
                return _.merge(edge, {
                    visible: visible
                });
            });

            // broadcast event for d3 directive to reset the visibility for the
            // graph
            $scope.$broadcast("d3graph_filter");
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

        // broadcast event for d3 directive to redraw the graph
        $scope.$broadcast("d3graph_redraw");
    }

    function onClick(d) {
        let state = DiscourseNode.get(d.label).state;
        $state.go(state, {
            id: d.id
        });
    }
});
