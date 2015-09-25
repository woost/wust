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

    let saveOnEnter = true;

    vm.sidebar = SidebarService;
    vm.editList = EditService.list;
    vm.edit = EditService.edit;
    vm.editNewPost = editNewPost;
    vm.options = EditService.scratchpad;

    vm.newPost = {
        title: ""
    };

    function editNewPost() {
        let session = EditService.edit(vm.newPost, 0, true);
        session.tags = angular.copy(ContextService.currentContexts);

        vm.newPost.title = "";

        if (saveOnEnter) {
            session.save();
        }
    }
}
