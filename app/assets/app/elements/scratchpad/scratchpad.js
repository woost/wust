angular.module("wust.components").directive("scratchpad", scratchpad);

scratchpad.$inject = [];

function scratchpad() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "assets/app/elements/scratchpad/scratchpad.html",
        scope: true,
        controller: scratchpadCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

scratchpadCtrl.$inject = ["EditService", "LeftSideService"];

function scratchpadCtrl(EditService, LeftSideService) {
    let vm = this;

    vm.leftSide = LeftSideService;
    vm.editList = EditService.list;
    vm.edit = EditService.edit;
    vm.editNewPost = editNewPost;

    vm.newPost = {
        title: ""
    };

    function editNewPost() {
        EditService.edit(vm.newPost);
        vm.newPost.title = "";
    }
}
