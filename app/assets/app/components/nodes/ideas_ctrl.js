angular.module("wust.components").controller("IdeasCtrl", IdeasCtrl);

IdeasCtrl.$inject = ["$stateParams", "Idea", "DiscourseNode", "DiscourseNodeList", "DiscourseNodeCrate"];

function IdeasCtrl($stateParams, Idea, DiscourseNode, DiscourseNodeList, DiscourseNodeCrate) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Idea;
    let node = Idea.$find($stateParams.id);
    vm.node = DiscourseNodeCrate(node);
    vm.top = DiscourseNodeList.write.Goal(node.goals.$search())
        .nested(DiscourseNodeList.write.ProArgument, "pros")
        .nested(DiscourseNodeList.write.ConArgument, "cons");
    vm.left = DiscourseNodeList.write.Problem(node.problems.$search())
        .nested(DiscourseNodeList.write.ProArgument, "pros")
        .nested(DiscourseNodeList.write.ConArgument, "cons");
    vm.bottom = DiscourseNodeList.write.Idea(node.ideas.$search());
}
