angular.module("wust.api").factory("Graph", Graph);

Graph.$inject = ["restmod"];

function Graph(restmod) {
    return restmod.model().mix({
        nodes: {
            decode: function(nodes) {
                //TODO: why does chaining not work?p
                //_(nodes).select(n => n.hyperEdge).each(n => n.title = n.label.toLowerCase());
                _.each(_.select(nodes, n => n.hyperEdge), n => n.title = n.label.toLowerCase());
                return nodes;
            }
        },
        edges: {
            decode: function(edges) {
                // TODO: not efficient
                // we need to reference nodes via their index in the nodes array, because d3 is weird.
                return _.map(edges, edge => {
                    return {
                        source: _.findIndex(this.nodes, {
                            id: edge.startId
                        }),
                        target: _.findIndex(this.nodes, {
                            id: edge.endId
                        }),
                        title: edge.label.toLowerCase(),
                        label: edge.label
                    };
                });
            }
        }
    }).single("/graph");
}
