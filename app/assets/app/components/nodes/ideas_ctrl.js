angular.module("wust.components").controller("IdeasCtrl", IdeasCtrl);

IdeasCtrl.$inject = ["$stateParams", "Idea", "DiscourseNode", "DiscourseNodeList"];

function IdeasCtrl($stateParams, Idea, DiscourseNode, DiscourseNodeList) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Idea;
    vm.node = Idea.$find($stateParams.id);
    vm.top = DiscourseNodeList.Goal(vm.node.goals)
        .nested(DiscourseNodeList.ProArgument, "pros")
        .nested(DiscourseNodeList.ConArgument, "cons");
    vm.left = DiscourseNodeList.Problem(vm.node.problems)
        .nested(DiscourseNodeList.ProArgument, "pros")
        .nested(DiscourseNodeList.ConArgument, "cons");
    vm.bottom = DiscourseNodeList.Idea(vm.node.ideas);
}
