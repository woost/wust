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

scratchpadCtrl.$inject = ["Session", "EditService", "SidebarService", "ContextService"];

function scratchpadCtrl(Session, EditService, SidebarService, ContextService) {
    let vm = this;

    let saveOnEnter = true;

    Session.marks.search().then(posts => {
        posts.forEach(post => EditService.edit(post, true));
    });

    vm.sidebar = SidebarService;
    vm.editList = EditService.list;
    vm.edit = EditService.edit;
    vm.editNewPost = editNewPost;
    vm.options = EditService.scratchpad;

    vm.newPost = {
        title: ""
    };

    function editNewPost() {
        let session = EditService.edit(vm.newPost, true, 0);
        session.tags = angular.copy(ContextService.currentContexts);

        vm.newPost.title = "";

        if (saveOnEnter) {
            session.save();
        }
    }
}
