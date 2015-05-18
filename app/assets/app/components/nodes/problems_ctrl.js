angular.module("wust.components").controller("ProblemsCtrl", ProblemsCtrl);

ProblemsCtrl.$inject = ["$stateParams", "Problem", "DiscourseNode", "DiscourseNodeList"];

function ProblemsCtrl($stateParams, Problem, DiscourseNode, DiscourseNodeList) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Problem;
    vm.node = Problem.$find($stateParams.id);
    vm.top = DiscourseNodeList.write.Goal(vm.node.goals);
    vm.left = DiscourseNodeList.write.Problem(vm.node.causes, "Causes");
    vm.right = DiscourseNodeList.write.Problem(vm.node.consequences, "Consequences");
    vm.bottom = DiscourseNodeList.write.Idea(vm.node.ideas)
        .nested(DiscourseNodeList.write.ProArgument, "pros")
        .nested(DiscourseNodeList.write.ConArgument, "cons");
}
