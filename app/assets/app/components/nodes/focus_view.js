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

        vm.node.model.$then(data => {
            NodeHistory.add(data);
        });

        // callbacks for removing/updating the focused node
        vm.removeFocused = _.wrap(vm.node.model, removeFocused);
        vm.updateFocused = _.wrap(vm.node.model, updateFocused);

        // register for events for the current node, as well as all connected lists
        vm.node.subscribe();
        _.each(_.compact([vm.left, vm.right, vm.bottom, vm.top]), list => list.subscribe());

    }

    function removeFocused(node) {
        node.$destroy().$then(() => {
            NodeHistory.remove(node.id);
            humane.success("Removed node");
            $state.go("browse");
        });
    }

    function updateFocused(node) {
        return node.$save().$then(() => {
            humane.success("Updated node");
        }).$promise;
    }
}
