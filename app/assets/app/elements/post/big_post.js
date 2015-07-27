angular.module("wust.elements").directive("bigPost", bigPost);

bigPost.$inject = [];

function bigPost() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/post/big_post.html",
        scope: {
            node: "="
        },
        controller: bigPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

bigPostCtrl.$inject = ["$state", "LeftSideService", "EditService"];

//TODO: we are using the markdown directive directly and also allow to enter zen
//mode. both directives will lead to parsing the markdown description, which is
//not needed. zen mode should reuse the parsed description here.
function bigPostCtrl($state, LeftSideService, EditService) {
    let vm = this;

    vm.updateFocused = updateFocused;

    function updateFocused() {
        EditService.edit(vm.node);
        LeftSideService.visible = true;
    }
}
