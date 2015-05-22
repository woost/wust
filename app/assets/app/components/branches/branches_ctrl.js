angular.module("wust.components").controller("BranchesCtrl", BranchesCtrl);

BranchesCtrl.$inject = ["ConnectedComponents", "$stateParams", "DiscourseNodeList"];

function BranchesCtrl(ConnectedComponents, $stateParams, DiscourseNodeList) {
    let vm = this;

    vm.component = {};
    ConnectedComponents.$find($stateParams.id).$then(data => {
        vm.component = data;
        vm.component.rootId = $stateParams.id;
        vm.nodeList = DiscourseNodeList.Any(data.nodes, "Nodes");
    });
}
