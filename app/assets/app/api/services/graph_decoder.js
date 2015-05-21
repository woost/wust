angular.module("wust.api").service("GraphDecoder", GraphDecoder);

GraphDecoder.$inject = [];

function GraphDecoder() {
    this.decodeNodes = decodeNodes;
    this.decodeEdges = decodeEdges;

    function decodeNodes(nodes) {
        _(nodes).select(n => n.hyperEdge).each(n => n.title = n.label.toLowerCase()).value();
        // sorting the nodes by hyperEdge puts normal nodes after hypernodes.
        // This ensures that normal nodes are always drawn on top of hypernodes.
        return _.sortBy(nodes, n => n.hyperEdge);
    }

    function decodeEdges(edges) {
        // we need to reference nodes via their index in the nodes array, because d3 is weird.
        let nodeMap = _(this.nodes).map((n, i) => {
            return {
                [n.id]: i
            };
        }).reduce(_.merge);
        return _(edges).map(edge => {
            // this.nodes refers to the restmod model
            return validateEdge(this.nodes, {
                source: nodeMap[edge.startId],
                target: nodeMap[edge.endId],
                title: edge.label.toLowerCase(),
                label: edge.label
            });
        }).compact().value();
    }

    function validateEdge(nodes, edge) {
        if (edge.source === undefined || edge.target === undefined) {
            let source = nodes[edge.source];
            let target = nodes[edge.target];
            let slabel = source === undefined ? "undefined" : source.label;
            let tlabel = target === undefined ? "undefined" : target.label;
            console.warn(`Node missing for edge: (${slabel}) -[${edge.label}]-> (${tlabel})`);
            return undefined;
        } else {
            return edge;
        }
    }
}
