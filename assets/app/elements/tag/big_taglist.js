angular.module("wust.elements").directive("bigTaglist", bigTaglist);

bigTaglist.$inject = [];

function bigTaglist() {
    return {
        restrict: "A",
        templateUrl: "elements/tag/big_taglist.html",
        scope: {
            node: "="
        },
        controller: bigTaglistCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

bigTaglistCtrl.$inject = [];

function bigTaglistCtrl() {
    let vm = this;
}
