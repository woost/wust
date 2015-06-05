angular.module("wust.discourse").directive("bigPost", bigPost);

bigPost.$inject = ["$state", "HistoryService", "EditService", "DiscourseNode"];

function bigPost($state, HistoryService, EditService, DiscourseNode) {
    return {
        restrict: "A",
        templateUrl: "assets/app/discourse/directives/big_post.html",
        scope: {
            node: "="
        },
        link: link
    };

    function link(scope) {
        // we are viewing details about a node, so add it to the nodehistory
        HistoryService.add(scope.node);

        scope.nodeInfo = DiscourseNode.Post;

        // callbacks for removing/updating the focused node
        scope.removeFocused = removeFocused;
        scope.updateFocused = updateFocused;

        function removeFocused() {
            scope.node.$destroy().$then(() => {
                HistoryService.remove(scope.node.id);
                humane.success("Removed node");
                $state.go("browse");
            });
        }

        function updateFocused() {
            EditService.editExisting(scope.node);
        }
    }
}
