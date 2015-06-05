angular.module("wust.discourse").directive("bigTaglist", bigTaglist);

bigTaglist.$inject = [];

function bigTaglist() {
    return {
        restrict: "A",
        templateUrl: "assets/app/discourse/directives/big_taglist.html",
        scope: {
            tags: "="
        },
        link: link
    };

    function link(scope) {
    }
}
