angular.module("wust.api").service("GraphDecoder", GraphDecoder);

GraphDecoder.$inject = [];

function GraphDecoder() {
    this.decodeNodes = decodeNodes;
    this.decodeEdges = decodeEdges;
    this.refreshIndex = refreshIndex;

    function decodeNodes(apiNodes) {
        //TODO: shouldn't the api return properly formatted nodes?
        _(apiNodes).select(n => n.hyperEdge).each(n => n.title = n.label.toLowerCase()).value();

        // sorting the nodes by hyperEdge puts normal nodes after hypernodes.
        // This ensures that normal nodes are always drawn on top of hypernodes.
        return _.sortBy(apiNodes, n => n.hyperEdge);
    }

    function decodeEdges(apiEdges) {
        let edges = _(apiEdges).map(edge => {
            // this.nodes refers to the restmod model
            return _.merge(edge, {
                // TODO: should be sent by the api
                title: edge.label.toLowerCase(),
                label: edge.label
            });
        }).value();

        return edges;
    }

    function refreshIndex() {
        // clear all neighbour information
        _.each(this.nodes, n => {
            n.inRelations = [];
            n.outRelations = [];
            Object.defineProperty(n, "relations", {
                get: function() {
                    return this.inRelations.concat(this.outRelations);
                }
            });
            Object.defineProperty(n, "inNeighbours", {
                get: function() {
                    return _.map(this.inRelations, r => r.source);
                }
            });
            Object.defineProperty(n, "outNeighbours", {
                get: function() {
                    return _.map(this.outRelations, r => r.target);
                }
            });
            Object.defineProperty(n, "neighbours", {
                get: function() {
                    return this.inNeighbours.concat(this.outNeighbours);
                }
            });
            n.component = function() {
                let visited = new Set();

                function findNeighbours(node) {
                    if (visited.has(node))
                        return;

                    visited.add(node);
                    _.each(node.neighbours, findNeighbours);
                }

                findNeighbours(this);
                return Array.from(visited);
            };
        });

        // hehe
        _.each(this.nodes, n => {
            // the id is a string, thus it won't be iterable but you can still
            // lookup nodes via their id: nodes[0] = first node, nodes["adsaf-c32"] = node with id "adsaf-c32"
            this.nodes[n.id] = n;
        });

        // reinitialize neighbours
        _.each(this.edges, e => {
            e.source = this.nodes[e.startId];
            e.target = this.nodes[e.endId];
            e.source.outRelations.push(e);
            e.target.inRelations.push(e);
        });
    }
}
