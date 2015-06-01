angular.module("wust.discourse").directive("discourseNodeCrate", discourseNodeCrate);

discourseNodeCrate.$inject = ["$state", "NodeHistory", "EditService"];

function discourseNodeCrate($state, NodeHistory, EditService) {
    return {
        restrict: "A",
        replace: false,
        templateUrl: "assets/app/discourse/directives/discourse_node_crate.html",
        scope: {
            node: "=",
            nodeInfo: "="
        },
        link: link
    };

    function link(scope) {
        // we are viewing details about a node, so add it to the nodehistory
        NodeHistory.add(scope.node);

        // callbacks for removing/updating the focused node
        scope.removeFocused = removeFocused;
        scope.updateFocused = updateFocused;

        function removeFocused() {
            scope.node.$destroy().$then(() => {
                NodeHistory.remove(scope.node.id);
                humane.success("Removed node");
                $state.go("browse");
            });
        }

        function updateFocused() {
            EditService.editExisting(scope.node);
        }
    }
}
