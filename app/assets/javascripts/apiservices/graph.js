angular.module("wust").factory("Graph", function(restmod) {
    return restmod.singleton("/graph", {
        edges: {
            decode: function(edges) {
                // TODO: not efficient
                // we need to reference nodes via their index in the nodes array, because d3 is weird.
                return edges.map(edge => _.merge(edge, {
                    source: _.findIndex(this.nodes, {
                        id: edge.from
                    }),
                    target: _.findIndex(this.nodes, {
                        id: edge.to
                    }),
                    label: edge.label.toLowerCase()
                }));
            }
        }
    });
});
