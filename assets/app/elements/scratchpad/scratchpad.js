angular.module("wust.elements").directive("scratchpad", scratchpad);

scratchpad.$inject = [];

function scratchpad() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "elements/scratchpad/scratchpad.html",
        scope: true,
        controller: scratchpadCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

scratchpadCtrl.$inject = ["EditService", "SidebarService", "ContextService"];

function scratchpadCtrl(EditService, SidebarService, ContextService) {
    let vm = this;

    vm.sidebar = SidebarService;
    vm.editList = EditService.list;
    vm.edit = EditService.edit;
    vm.editNewPost = editNewPost;

    vm.newPost = {
        title: "",
        tags: []
    };

    function editNewPost() {
        vm.newPost.tags = ContextService.currentContexts;
        EditService.edit(vm.newPost);
        vm.newPost.title = "";
    }
}
