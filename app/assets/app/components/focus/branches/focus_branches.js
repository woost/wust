angular.module("wust.components").directive("focusBranches", focusBranches);

focusBranches.$inject = [];

function focusBranches() {
    return {
        restrict: "A",
        templateUrl: "assets/app/components/focus/branches/branches.html",
        scope: {
            component: "="
        },
        controller: BranchesCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

BranchesCtrl.$inject = ["DiscourseNodeList"];

function BranchesCtrl(DiscourseNodeList) {
    let vm = this;
}
