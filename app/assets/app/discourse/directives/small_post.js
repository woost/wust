angular.module("wust.discourse").directive("smallPost", smallPost);

smallPost.$inject = ["DiscourseNode"];

function smallPost(DiscourseNode) {
    return {
        restrict: "A",
        replace: false,
        templateUrl: "assets/app/discourse/directives/small_post.html",
        scope: {
            node: "=",
        },
        link: link
    };

    function link(scope) {
        scope.nodeInfo = DiscourseNode.Post;
        console.log(scope.nodeInfo);
    }
}
