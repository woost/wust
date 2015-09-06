angular.module("wust.elements").directive("bigPost", bigPost);

bigPost.$inject = [];

function bigPost() {
    return {
        restrict: "A",
        templateUrl: "elements/post/big_post.html",
        scope: {
            node: "="
        },
        controller: bigPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

bigPostCtrl.$inject = ["SidebarService", "Post", "EditService", "Session", "ModalEditService", "ContextService"];

//TODO: we are using the markdown directive directly and also allow to enter zen
//mode. both directives will lead to parsing the markdown description, which is
//not needed. zen mode should reuse the parsed description here.
function bigPostCtrl(SidebarService, Post, EditService, Session, ModalEditService, ContextService) {
    let vm = this;

    vm.editNode = EditService.createSession(vm.node);
    vm.editChanges = Post.$buildRaw(vm.node).requestsEdit.$search();
    vm.tagChanges = Post.$buildRaw(vm.node).requestsTags.$search();
    vm.replyTo = replyTo;
    vm.onSave = onSave;
    vm.onApply = onApply;
    vm.editMode = false;
    vm.currentContexts = ContextService.currentContexts;

    function onSave(response) {
        vm.editMode = false;
        if (response) {
            response.requestsEdit.forEach(req => vm.editChanges.push(req));
            response.requestsTags.forEach(req => vm.tagChanges.push(req));
        }
    }

    function onApply(response) {
        vm.node.title = response.title;
        vm.node.description = response.description;
        response.tags.forEach(t => vm.node.tags.push(t));
        vm.editNode.apply(vm.node);
    }

    function replyTo() {
        ModalEditService.show();
        ModalEditService.currentNode.setReference(vm.editNode);
    }
}
