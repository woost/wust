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

    //TODO: implement own list
    vm.nodeList = DiscourseNodeList.read(vm.component.nodes);
    //this is not supported by the discourse_node_list anymore
    vm.nodeList.model.orderProperty = "line";
}
