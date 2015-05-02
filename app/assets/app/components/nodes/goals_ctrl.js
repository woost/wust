angular.module("wust.components").controller("GoalsCtrl", function($scope, $stateParams, Goal, DiscourseNode, DiscourseNodeList) {
    $scope.nodeInfo = DiscourseNode.Goal;
    $scope.node = Goal.$find($stateParams.id);
    $scope.top = DiscourseNodeList.Goal($scope.node.goals);
    $scope.left =  DiscourseNodeList.Problem($scope.node.problems);
    $scope.bottom = DiscourseNodeList.Idea($scope.node.ideas)
        .nested(DiscourseNodeList.ProArgument, "pros")
        .nested(DiscourseNodeList.ConArgument, "cons");
});
