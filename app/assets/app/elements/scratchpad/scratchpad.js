angular.module("wust.elements").directive("scratchpad", scratchpad);

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

scratchpadCtrl.$inject = ["EditService", "SidebarService"];

function scratchpadCtrl(EditService, SidebarService) {
    let vm = this;

    vm.sidebar = SidebarService;
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
