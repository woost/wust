angular.module("wust.discourse").directive("discourseNodeCrate", discourseNodeCrate);

discourseNodeCrate.$inject = [];

function discourseNodeCrate() {
    return {
        restrict: "A",
        replace: false,
        templateUrl: "assets/app/discourse/directives/discourse_node_crate.html",
        scope: {
            node: "=",
            nodeInfo: "="
        },
        controller: discourseNodeCrateCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

discourseNodeCrateCtrl.$inject = ["$scope", "$state", "NodeHistory"];

function discourseNodeCrateCtrl($scope, $state, NodeHistory) {
    let vm = this;

    // we are viewing details about a node, so add it to the nodehistory
    vm.node.$then(data => {
        NodeHistory.add(data);
    });

    // callbacks for removing/updating the focused node
    vm.removeFocused = removeFocused;
    vm.updateFocused = updateFocused;

    function removeFocused() {
        vm.node.$destroy().$then(() => {
            NodeHistory.remove(vm.node.id);
            humane.success("Removed node");
            $state.go("browse");
        });
    }

    function updateFocused(field, data) {
        let node = angular.copy(vm.node).$extend({
            [field]: data
        });
        return node.$save().$then(() => {
            humane.success("Updated node");
        }).$asPromise();
    }
}
