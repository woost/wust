angular.module("wust.elements").directive("smallPost", smallPost);

smallPost.$inject = [];

function smallPost() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/post/small_post.html",
        scope: {
            node: "=",
            valid: "="
        },
        controller: smallPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

smallPostCtrl.$inject = ["DiscourseNode"];

function smallPostCtrl(DiscourseNode) {
    let vm = this;

    vm.valid = vm.valid === undefined ? true : vm.valid;

    // If you are here because the link does not update, when a node is created
    // and gets an id: you need to update the href when the id changes with a
    // watcher. as long as we do not need this, we just do this once.
    vm.href = DiscourseNode.Post.getHref(vm.node.id);
}
