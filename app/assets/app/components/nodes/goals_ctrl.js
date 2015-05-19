angular.module("wust.components").controller("GoalsCtrl", GoalsCtrl);

GoalsCtrl.$inject = ["$stateParams", "Goal", "DiscourseNode", "DiscourseNodeList", "DiscourseNodeCrate"];

function GoalsCtrl($stateParams, Goal, DiscourseNode, DiscourseNodeList, DiscourseNodeCrate) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Goal;
    let node = Goal.$find($stateParams.id);
    vm.node = DiscourseNodeCrate(node);
    vm.top = DiscourseNodeList.write.Goal(node.goals);
    vm.left =  DiscourseNodeList.write.Problem(node.problems);
    vm.bottom = DiscourseNodeList.write.Idea(node.ideas)
        .nested(DiscourseNodeList.write.ProArgument, "pros")
        .nested(DiscourseNodeList.write.ConArgument, "cons");
}
