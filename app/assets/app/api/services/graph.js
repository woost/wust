angular.module("wust.api").factory("Graph", function(restmod) {
    return restmod.model().mix({
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
});
