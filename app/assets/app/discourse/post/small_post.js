angular.module("wust.discourse").directive("smallPost", smallPost);

smallPost.$inject = [];

function smallPost() {
    return {
        restrict: "A",
        templateUrl: "assets/app/discourse/post/small_post.html",
        scope: {
            node: "=",
        },
        controller: smallPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

smallPostCtrl.$inject = ["DiscourseNode"];

function smallPostCtrl(DiscourseNode) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Post;
}
