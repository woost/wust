angular.module("wust.discourse").directive("discourseNodeCrate", discourseNodeCrate);

discourseNodeCrate.$inject = ["$state", "NodeHistory"];

function discourseNodeCrate($state, NodeHistory) {
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
        scope.node.$then(data => {
            NodeHistory.add(data);
        });

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

        function updateFocused(field, data) {
            let node = angular.copy(scope.node).$extend({
                [field]: data
            });
            return node.$save().$then(() => {
                humane.success("Updated node");
            }).$asPromise();
        }
    }
}
