angular.module("wust.components").controller("ProblemsCtrl", ProblemsCtrl);

ProblemsCtrl.$inject = ["$stateParams", "Problem", "DiscourseNode", "DiscourseNodeList", "DiscourseNodeCrate"];

function ProblemsCtrl($stateParams, Problem, DiscourseNode, DiscourseNodeList, DiscourseNodeCrate) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Problem;
    let node = Problem.$find($stateParams.id);
    vm.node = DiscourseNodeCrate(node);
    vm.top = DiscourseNodeList.write.Goal(node.goals);
    vm.left = DiscourseNodeList.write.Problem(node.causes, "Causes");
    vm.right = DiscourseNodeList.write.Problem(node.consequences, "Consequences");
    vm.bottom = DiscourseNodeList.write.Idea(node.ideas)
        .nested(DiscourseNodeList.write.ProArgument, "pros")
        .nested(DiscourseNodeList.write.ConArgument, "cons");
}
