angular.module("wust.api").factory("ConnectedComponents", ConnectedComponents);

ConnectedComponents.$inject = ["restmod", "GraphDecoder"];

function ConnectedComponents(restmod, GraphDecoder) {
    return restmod.model("/components").mix({
        $hooks: {
            "after-fetch": GraphDecoder.refreshHook
        },
        $extend: {
            Record: GraphDecoder.extendedModel
        }
    }, {
        nodes: {
            decode: GraphDecoder.decodeNodes
        },
        edges: {
            decode: GraphDecoder.decodeEdges
        }
    });
}
