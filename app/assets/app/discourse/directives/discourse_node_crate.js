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
        // register for node events
        scope.node.subscribe();

        // we are viewing details about a node, so add it to the nodehistory
        scope.node.model.$then(data => {
            NodeHistory.add(data);
        });

        // callbacks for removing/updating the focused node
        scope.removeFocused = removeFocused;
        scope.updateFocused = updateFocused;

        function removeFocused() {
            scope.node.model.$destroy().$then(() => {
                NodeHistory.remove(scope.node.model.id);
                humane.success("Removed node");
                $state.go("browse");
            });
        }

        function updateFocused(field, data) {
            let node = angular.copy(scope.node.model).$extend({
                [field]: data
            });
            return node.$save().$then(() => {
                humane.success("Updated node");
            }).$promise;
        }
    }
}
