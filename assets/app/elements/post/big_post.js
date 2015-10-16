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

bigPostCtrl.$inject = ["$state", "Post", "ModalEditService", "ContextService", "Auth"];

//TODO: we are using the markdown directive directly and also allow to enter zen
//mode. both directives will lead to parsing the markdown description, which is
//not needed. zen mode should reuse the parsed description here.
function bigPostCtrl($state, Post, ModalEditService, ContextService, Auth) {
    let vm = this;

    vm.node = vm.component.rootNode;

    vm.Auth = Auth;

    vm.showAuthor = true;

    vm.changeRequests = Post.$buildRaw(_.pick(vm.node, "id")).requests.$search();
    vm.replyTo = replyTo;
    vm.onSave = onSave;
    vm.onCancel = onCancel;
    vm.onDelete = onDelete;
    vm.onApply = onApply;
    vm.onTagApply = onTagApply;
    vm.onDeleteApply = onDeleteApply;
    vm.editMode = false;
    vm.nodeHasContext = () => _.any(vm.node.tags, "isContext");

    vm.nodeIsDeleted = false;

    // ContextService.setNodeContext(vm.node);

    function onSave(response) {
        vm.editMode = false;
        vm.changeRequests = _.uniq(response.requestsEdit.concat(response.requestsTags).concat(vm.changeRequests), "id").filter(r => !r.status);
    }

    function onCancel() {
        vm.editMode = false;
    }

    function onDelete(response) {
        vm.editMode = false;
        if (response.$response.status === 204) // NoContent response means node was instantly deleted
            onDeleteApply();
        else { // there should be deleterequests
            //TODO: why can i not reference them via response.requestsDelete like in onSave
            vm.changeRequests = _.uniq(response.$response.data.requestsDelete.concat(vm.changeRequests), "id").filter(r => !r.status);
        }
    }

    function onApply(node) {
        vm.node.title = node.title;
        vm.node.description = node.description;
    }

    function onDeleteApply() {
        vm.nodeIsDeleted = true;
    }

    function onTagApply(change) {
        if (change.isRemove) {
            if (change.classifications.length > 0) {
                let exist = _.find(vm.node.tags, _.pick(change.tag, "id"));
                if (exist)
                    change.classifications.forEach(c => _.remove(exist.classifications, _.pick(c, "id")));
            } else {
                _.remove(vm.node.tags, _.pick(change.tag, "id"));
            }
        } else {
            let exist = _.find(vm.node.tags, _.pick(change.tag, "id"));
            if (exist) {
                Array.prototype.push.apply(exist.classifications, change.classifications);
            } else {
                change.tag.classifications = change.classifications;
                vm.node.tags.push(change.tag);
            }
        }
    }

    function replyTo() {
        ModalEditService.show(vm.node);
    }
}
