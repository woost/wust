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

bigPostCtrl.$inject = ["SidebarService", "Post", "EditService", "ModalEditService"];

//TODO: we are using the markdown directive directly and also allow to enter zen
//mode. both directives will lead to parsing the markdown description, which is
//not needed. zen mode should reuse the parsed description here.
function bigPostCtrl(SidebarService, Post, EditService, ModalEditService) {
    let vm = this;

    vm.editNode = EditService.edit(vm.node);
    vm.editChanges = Post.$buildRaw(vm.node).requestsEdit.$search();
    vm.tagChanges = Post.$buildRaw(vm.node).requestsTags.$search();
    vm.replyTo = replyTo;
    vm.onSave = onSave;
    vm.onApply = onApply;
    vm.editMode = false;
    vm.upvoteTag = upvoteTag;
    vm.onTagApply = onTagApply;
    vm.nodeHasContext = () => _.any(vm.node.tags, "isContext");

    function onSave(response) {
        vm.editMode = false;
        if (response) {
            vm.editChanges = _.uniq(response.requestsEdit.concat(vm.editChanges), "id").filter(r => !r.applied);
            vm.tagChanges = _.uniq(response.requestsTags.concat(vm.tagChanges), "id").filter(r => !r.applied);
        }
    }

    //TODO: need to unvote
    //TODO: semnatic downvote on post
    function upvoteTag(tag) {
        Post.$buildRaw(_.pick(vm.node, "id")).tags.$buildRaw(_.pick(tag, "id")).up.$create().$then(data => {
            tag.vote = data.vote;
            humane.success("Upvoted post in context");
        }, resp => {
            humane.error(resp.$response.data);
        });
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
