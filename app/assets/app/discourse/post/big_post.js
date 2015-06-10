angular.module("wust.discourse").directive("bigPost", bigPost);

bigPost.$inject = [];

function bigPost() {
    return {
        restrict: "A",
        templateUrl: "assets/app/discourse/post/big_post.html",
        scope: {
            node: "="
        },
        controller: bigPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

bigPostCtrl.$inject = ["$state", "HistoryService", "EditService"];

//TODO: we are using the markdown directive directly and also allow to enter zen
//mode. both directives will lead to parsing the markdown description, which is
//not needed. zen mode should reuse the parsed description here.
function bigPostCtrl($state, HistoryService, EditService) {
    let vm = this;

    // callbacks for removing/updating the focused node
    vm.removeFocused = removeFocused;
    vm.updateFocused = updateFocused;

    function removeFocused() {
        vm.node.$destroy().$then(() => {
            HistoryService.remove(vm.node.id);
            humane.success("Removed node");
            $state.go("browse");
        });
    }

    function updateFocused() {
        EditService.edit(vm.node);
    }
}
