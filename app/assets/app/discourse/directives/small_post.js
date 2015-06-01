angular.module("wust.discourse").directive("smallPost", smallPost);

smallPost.$inject = [];

function smallPost() {
    return {
        restrict: "A",
        replace: false,
        templateUrl: "assets/app/discourse/directives/small_post.html",
        scope: {
            node: "=",
            nodeInfo: "="
        },
        link: link
    };

    function link(scope) {
    }
}
