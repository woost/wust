angular.module("wust.api").factory("Graph", Graph);

Graph.$inject = ["restmod"];

function Graph(restmod) {
    return restmod.model().mix({
        nodes: {
            decode: function(nodes) {
                // set title to lowercase label if there is no title
                return _.each(nodes, node => node.title = node.title || node.label.toLowerCase());
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
                        label: edge.label.toLowerCase()
                    };
                });
            }
        }
    }).single("/graph");
}
