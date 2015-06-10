angular.module("wust.discourse").directive("bigTaglist", bigTaglist);

bigTaglist.$inject = [];

function bigTaglist() {
    return {
        restrict: "A",
        templateUrl: "assets/app/discourse/tag/big_taglist.html",
        scope: {
            tags: "="
        },
        controller: bigTaglistCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

bigTaglistCtrl.$inject = ["DiscourseNode"];

function bigTaglistCtrl(DiscourseNode) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Tag;
}
