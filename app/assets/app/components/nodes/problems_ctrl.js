angular.module("wust.components").controller("ProblemsCtrl", ProblemsCtrl);

ProblemsCtrl.$inject = ["$stateParams", "Problem", "DiscourseNode", "DiscourseNodeList"];

function ProblemsCtrl($stateParams, Problem, DiscourseNode, DiscourseNodeList) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Problem;
    vm.node = Problem.$find($stateParams.id);
    vm.top = DiscourseNodeList.Goal(vm.node.goals);
    vm.left = DiscourseNodeList.Problem(vm.node.causes, "Causes");
    vm.right = DiscourseNodeList.Problem(vm.node.consequences, "Consequences");
    vm.bottom = DiscourseNodeList.Idea(vm.node.ideas)
        .nested(DiscourseNodeList.ProArgument, "pros")
        .nested(DiscourseNodeList.ConArgument, "cons");
}
