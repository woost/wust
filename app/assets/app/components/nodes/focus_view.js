angular.module("wust.components").directive("focusView", focusView);

focusView.$inject = ["$state", "$rootScope", "$q", "NodeHistory"];

function focusView($state, $rootScope, $q, NodeHistory) {
    return {
        restrict: "A",
        replace: false,
        scope: true,
        link: link
    };

    function link($scope) {
        let vm = $scope.vm;

        // callbacks for removing/updating the focused node
        vm.removeFocused = removeFocused;
        vm.updateFocused = updateFocused;

        // we are visiting the focus view of this node, thus the node is added to the NodeHistory
        vm.node.model.$then(data => {
            NodeHistory.add(data);
        });

        // register for events for the current node, as well as all connected lists
        // so we assume to get vm.node and vm.left/right/bottom/top as DiscourceNodeCrate and DiscourseNodeLists
        vm.node.subscribe();
        _.each(_.compact([vm.left, vm.right, vm.bottom, vm.top]), list => list.subscribe());

        function removeFocused() {
            vm.node.model.$destroy().$then(() => {
                NodeHistory.remove(vm.node.model.id);
                humane.success("Removed node");
                $state.go("browse");
            });
        }

        function updateFocused(field, data) {
            let node = angular.copy(vm.node.model).$extend({
                [field]: data
            });
            return node.$save().$then(() => {
                humane.success("Updated node");
            }).$promise;
        }
    }
}
