angular.module("wust.components").controller("GoalsCtrl", GoalsCtrl);

GoalsCtrl.$inject = ["$stateParams", "Goal", "DiscourseNode", "DiscourseNodeList"];

function GoalsCtrl($stateParams, Goal, DiscourseNode, DiscourseNodeList) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Goal;
    vm.node = Goal.$find($stateParams.id);
    vm.top = DiscourseNodeList.write.Goal(vm.node.goals);
    vm.left =  DiscourseNodeList.write.Problem(vm.node.problems);
    vm.bottom = DiscourseNodeList.write.Idea(vm.node.ideas)
        .nested(DiscourseNodeList.write.ProArgument, "pros")
        .nested(DiscourseNodeList.write.ConArgument, "cons");
}
