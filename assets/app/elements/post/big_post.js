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

bigPostCtrl.$inject = ["SidebarService", "Connectable", "Post", "EditService", "ModalEditService"];

//TODO: we are using the markdown directive directly and also allow to enter zen
//mode. both directives will lead to parsing the markdown description, which is
//not needed. zen mode should reuse the parsed description here.
function bigPostCtrl(SidebarService, Connectable, Post, EditService, ModalEditService) {
    let vm = this;

    vm.editNode = EditService.createSession(vm.node);
    vm.editChanges = Post.$buildRaw(vm.node).requestsEdit.$search();
    vm.addTagChanges = Post.$buildRaw(vm.node).requestsAddTags.$search();
    vm.removeTagChanges = Post.$buildRaw(vm.node).requestsRemoveTags.$search();
    vm.replyTo = replyTo;
    vm.onSave = onSave;
    vm.onApply = onApply;
    vm.editMode = false;
    vm.upvoteTag = upvoteTag;
    vm.nodeHasContext = () => _.any(vm.node.tags, "isContext");

    function onSave(response) {
        vm.editMode = false;
        if (response) {
            response.requestsEdit.forEach(req => vm.editChanges.push(req));
            response.requestsAddTags.forEach(req => vm.addTagChanges.push(req));
            response.requestsRemoveTags.forEach(req => vm.removeTagChanges.push(req));
        }
    }

    //TODO: need to unvote
    //TODO: semnatic downvote on post
    function upvoteTag(tag) {
        Connectable.$buildRaw(_.pick(vm.node, "id")).tags.$buildRaw(_.pick(tag, "id")).up.$create().$then(() => {
            humane.success("Upvoted post in context");
        }, resp => {
            humane.error(resp.$response.data);
        });
    }

    function onApply(response) {
        vm.node.title = response.title;
        vm.node.description = response.description;
        vm.editNode.apply(vm.node);
    }

    function onTagAdd(tag) {
        vm.node.tags.push(tag);
        vm.editNode.apply(vm.node);
    }

    function onTagRemove(tag) {
        _.remove(vm.node.tags, _.pick(tag, "id"));
        vm.editNode.apply(vm.node);
    }

    function replyTo() {
        ModalEditService.show(vm.editNode);
    }
}
