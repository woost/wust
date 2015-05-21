angular.module("wust.components").controller("UntypedsCtrl", UntypedsCtrl);

UntypedsCtrl.$inject = ["$stateParams", "Untyped", "DiscourseNode", "DiscourseNodeList", "DiscourseNodeCrate"];

function UntypedsCtrl($stateParams, Untyped, DiscourseNode, DiscourseNodeList, DiscourseNodeCrate) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Untypeds;
    let node = Untyped.$find($stateParams.id);
    vm.node = DiscourseNodeCrate(node);
    vm.top = DiscourseNodeList.write.Untyped(node.prays.$search());
    vm.bottom = DiscourseNodeList.write.Untyped(node.responds.$search());
}
