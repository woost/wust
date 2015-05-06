angular.module("wust.components").controller("IdeasCtrl", IdeasCtrl);

IdeasCtrl.$inject = ["$scope", "$stateParams", "Idea", "DiscourseNode", "DiscourseNodeList"];

function IdeasCtrl($scope, $stateParams, Idea, DiscourseNode, DiscourseNodeList) {
    $scope.nodeInfo = DiscourseNode.Idea;
    $scope.node = Idea.$find($stateParams.id);
    $scope.top = DiscourseNodeList.Goal($scope.node.goals)
        .nested(DiscourseNodeList.ProArgument, "pros")
        .nested(DiscourseNodeList.ConArgument, "cons");
    $scope.left = DiscourseNodeList.Problem($scope.node.problems)
        .nested(DiscourseNodeList.ProArgument, "pros")
        .nested(DiscourseNodeList.ConArgument, "cons");
    $scope.bottom = DiscourseNodeList.Idea($scope.node.ideas);
}
