angular.module("wust.elements").directive("bigPost", bigPost);

bigPost.$inject = [];

function bigPost() {
    return {
        restrict: "A",
        templateUrl: "elements/post/big_post.html",
        scope: {
            component: "="
        },
        controller: bigPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

bigPostCtrl.$inject = ["Post", "EditService", "ModalEditService", "ContextService", "Auth"];

//TODO: we are using the markdown directive directly and also allow to enter zen
//mode. both directives will lead to parsing the markdown description, which is
//not needed. zen mode should reuse the parsed description here.
function bigPostCtrl(Post, EditService, ModalEditService, ContextService, Auth) {
    let vm = this;

    vm.node = vm.component.rootNode;

    vm.Auth = Auth;

    vm.editNode = EditService.edit(vm.node);
    vm.changeRequests = Post.$buildRaw(vm.node).requests.$search();
    vm.replyTo = replyTo;
    vm.onSave = onSave;
    vm.onApply = onApply;
    vm.onTagApply = onTagApply;
    vm.editMode = false;
    vm.nodeHasContext = () => _.any(vm.node.tags, "isContext");

    ContextService.setNodeContext(vm.node);

    function onSave(response) {
        vm.editMode = false;
        if (response) {
            vm.changeRequests = _.uniq(response.requestsEdit.concat(response.requestsTags).concat(vm.changeRequests), "id").filter(r => !r.status);
        }
    }

    function onApply(node) {
        vm.node.title = node.title;
        vm.node.description = node.description;
        vm.editNode.apply(vm.node);
    }

    function onTagApply(tag, isRemove) {
        if (isRemove)
            _.remove(vm.node.tags, _.pick(tag, "id"));
        else
            vm.node.tags.push(tag);

        vm.editNode.apply(vm.node);
    }

    function replyTo() {
        ModalEditService.show(vm.editNode);
    }
}
