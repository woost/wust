angular.module("wust.elements").directive("smallPost", smallPost);

smallPost.$inject = [];

function smallPost() {
    return {
        restrict: "A",
        templateUrl: "elements/post/small_post.html",
        scope: {
            node: "=",
            ignoreTags: "="
        },
        controller: smallPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

smallPostCtrl.$inject = ["Auth"];

function smallPostCtrl(Auth) {
    let vm = this;

    vm.Auth = Auth;
}

