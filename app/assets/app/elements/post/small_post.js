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
    vm.nodeInfo = DiscourseNode.Post;
}
