angular.module("wust.elements").directive("staticEditPost", staticEditPost);

staticEditPost.$inject = [];

function staticEditPost() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/post/static_edit_post.html",
        scope: {
            node: "="
        },
        controller: staticEditPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

staticEditPostCtrl.$inject = ["ZenService"];

// expects scope.node to be a session.
// used by the scratchpad which retrieves a list of sessions from the EditService.
function staticEditPostCtrl(ZenService) {
    let vm = this;

    vm.previewService = ZenService.create();
}
