angular.module("wust.discourse").directive("bigPost", bigPost);

bigPost.$inject = ["$state", "NodeHistory", "EditService", "DiscourseNode"];

function bigPost($state, NodeHistory, EditService, DiscourseNode) {
    return {
        restrict: "A",
        replace: false,
        templateUrl: "assets/app/discourse/directives/big_post.html",
        scope: {
            node: "="
        },
        link: link
    };

    function link(scope) {
        // we are viewing details about a node, so add it to the nodehistory
        NodeHistory.add(scope.node);

        scope.nodeInfo = DiscourseNode.Post;

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
