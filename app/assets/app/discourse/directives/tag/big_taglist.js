angular.module("wust.discourse").directive("bigTaglist", bigTaglist);

bigTaglist.$inject = [];

function bigTaglist() {
    return {
        restrict: "A",
        templateUrl: "assets/app/discourse/directives/tag/big_taglist.html",
        scope: {
            tags: "="
        },
        controller: bigTaglistCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

bigTaglistCtrl.$inject = [];

function bigTaglistCtrl($state, HistoryService, EditService, DiscourseNode) {
    let vm = this;
}
