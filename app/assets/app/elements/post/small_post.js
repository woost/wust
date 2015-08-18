angular.module("wust.elements").directive("smallPost", smallPost);

smallPost.$inject = [];

function smallPost() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/post/small_post.html",
        scope: {
            node: "=",
            onDisconnect: "=",
            noLink: "@"
        },
        controller: smallPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

smallPostCtrl.$inject = [];

function smallPostCtrl() {
    let vm = this;
}
