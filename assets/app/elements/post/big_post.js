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

bigPostCtrl.$inject = ["$state", "Post", "ModalEditService", "ContextService", "Auth", "HistoryService"];

//TODO: we are using the markdown directive directly and also allow to enter zen
//mode. both directives will lead to parsing the markdown description, which is
//not needed. zen mode should reuse the parsed description here.
function bigPostCtrl($state, Post, ModalEditService, ContextService, Auth, HistoryService) {
    let vm = this;

    vm.node = vm.component.rootNode;

    vm.Auth = Auth;

    vm.showAuthor = true;

    vm.changeRequests = Post.$buildRaw(_.pick(vm.node, "id")).requests.$search();
    vm.history = Post.$buildRaw(_.pick(vm.node, "id")).history.$search().$then(updateAuthors);
    vm.replyTo = replyTo;
    vm.onSave = onSave;
    vm.onCancel = onCancel;
    vm.onDelete = onDelete;
    vm.onApply = onApply;
    vm.onTagApply = onTagApply;
    vm.onDeleteApply = onDeleteApply;
    vm.editMode = false;
    vm.nodeHasContext = () => _.any(vm.node.tags, "isContext");

    updateAuthors();

    // ContextService.setNodeContext(vm.node);

    function updateAuthors() {
        vm.authors = _.uniq([vm.node.author].concat(vm.history.map(h => h.author).reverse()), "id");
    }

    function onSave(response) {
        vm.editMode = false;
        vm.changeRequests = _.uniq(response.requestsEdit.concat(response.requestsTags).concat(vm.changeRequests), "id").filter(r => r.status === 0);
        vm.history = _.uniq(response.requestsEdit.concat(response.requestsTags).filter(r => r.status > 0).concat(vm.history), "id");
        updateAuthors();
    }

    function onCancel() {
        vm.editMode = false;
    }

    function onDelete(response) {
        vm.editMode = false;
        if (response.$response.status !== 204) { // NoContent response means node was instantly deleted
            //TODO: why can i not reference them via response.requestsDelete like in onSave
            vm.changeRequests = _.uniq(response.$response.data.requestsDelete.concat(vm.changeRequests), "id").filter(r => !r.status);
        }
    }

    function onApply(change, node) {
        vm.node.title = node.title;
        vm.node.description = node.description;
        vm.history.unshift(change);
        updateAuthors();
    }

    function onDeleteApply() {
        HistoryService.removeFromCurrentView(vm.node.id);
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
        vm.history.unshift(change);
        updateAuthors();
    }

    function replyTo() {
        ModalEditService.show(vm.node);
    }
}
