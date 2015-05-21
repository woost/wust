angular.module("wust.api").factory("Graph", Graph);

Graph.$inject = ["restmod"];

function Graph(restmod) {
    return restmod.model().mix({
        nodes: {
            decode: function(nodes) {
                _(nodes).select(n => n.hyperEdge).each(n => n.title = n.label.toLowerCase()).value();
                return nodes;
            }
        },
        edges: {
            decode: function(edges) {
                let thisModel = this;
                function validateEdge(edge) {
                    if( edge.source === undefined || edge.target === undefined ) {
                        let source = thisModel.nodes[edge.source];
                        let target = thisModel.nodes[edge.target];
                        let slabel = source === undefined ? "undefined" : source.label;
                        let tlabel = target === undefined ? "undefined" : target.label;
                        console.warn(`Node missing for edge: (${slabel}) -[${edge.label}]-> (${tlabel})`);
                        return undefined;
                    } else {
                        return edge;
                    }
                }
                // we need to reference nodes via their index in the nodes array, because d3 is weird.
                let nodeMap = _(this.nodes).map((n,i) => {
                    return {
                        [n.id]: i
                    };
                }).reduce(_.merge);
                return _(edges).map(edge => {
                    return validateEdge({
                        source: nodeMap[edge.startId],
                        target: nodeMap[edge.endId],
                        title: edge.label.toLowerCase(),
                        label: edge.label
                    });
                }).compact().value();
            }
        }
    }).single("/graph");
}
