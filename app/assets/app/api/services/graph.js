angular.module("wust.api").factory("Graph", Graph);

Graph.$inject = ["restmod", "GraphDecoder"];

function Graph(restmod, GraphDecoder) {
    return restmod.model().mix({
        nodes: {
            decode: GraphDecoder.decodeNodes,
        },
        edges: {
            decode: GraphDecoder.decodeEdges
        }
    }).single("/graph");
}
