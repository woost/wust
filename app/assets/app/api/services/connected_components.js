angular.module("wust.api").factory("ConnectedComponents", ConnectedComponents);

ConnectedComponents.$inject = ["restmod", "GraphDecoder"];

function ConnectedComponents(restmod, GraphDecoder) {
    return restmod.model("/components").mix({
        nodes: {
            decode: GraphDecoder.decodeNodes,
        },
        edges: {
            decode: GraphDecoder.decodeEdges
        }
    });
}
