angular.module("wust.components").controller("GoalsCtrl", GoalsCtrl);

GoalsCtrl.$inject = ["$stateParams", "Goal", "DiscourseNode", "DiscourseNodeList"];

function GoalsCtrl($stateParams, Goal, DiscourseNode, DiscourseNodeList) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Goal;
    vm.node = Goal.$find($stateParams.id);
    vm.top = DiscourseNodeList.Goal(vm.node.goals);
    vm.left =  DiscourseNodeList.Problem(vm.node.problems);
    vm.bottom = DiscourseNodeList.Idea(vm.node.ideas)
        .nested(DiscourseNodeList.ProArgument, "pros")
        .nested(DiscourseNodeList.ConArgument, "cons");
}
