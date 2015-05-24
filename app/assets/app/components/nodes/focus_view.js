angular.module("wust.components").directive("focusView", focusView);

focusView.$inject = ["$state", "$rootScope", "$q", "NodeHistory"];

function focusView($state, $rootScope, $q, NodeHistory) {
    return {
        restrict: "A",
        replace: false,
        scope: true,
        link: link
    };

    function link(scope) {
        let vm = scope.vm;

        // register for events of all connected lists and the node itself
        vm.node.subscribe();
        _.each(_.compact([vm.left, vm.right, vm.bottom, vm.top]), list => list.subscribe());
    }
}
