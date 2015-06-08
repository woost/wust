angular.module("wust.components").directive("focusBranches", focusBranches);

focusBranches.$inject = [];

function focusBranches() {
    return {
        restrict: "A",
        templateUrl: "assets/app/components/focus/branches/branches.html",
        scope: {
            component: "=",
            rootId: "="
        },
        controller: BranchesCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

BranchesCtrl.$inject = ["DiscourseNodeList"];

function BranchesCtrl(DiscourseNodeList) {
    let vm = this;

    vm.component.$then(data => {
        //TODO: this is a workaround. Copy graph only once in branch view or wrap original graph
        data.branchData = data.branchData || angular.copy(data);
        vm.nodeList = DiscourseNodeList.read(data.branchData.nodes);
        vm.nodeList.model.orderProperty = "line";
    });
}
