angular.module("wust.components").controller("GoalsCtrl", function($scope, $stateParams, Goal, DiscourseNode, DiscourseNodeList) {
    $scope.nodeInfo = DiscourseNode.Goal;
    $scope.node = Goal.$find($stateParams.id);
    $scope.top = new DiscourseNodeList.Goal($scope.node.goals);
    $scope.left = new DiscourseNodeList.Problem($scope.node.problems);
    $scope.bottom = new DiscourseNodeList.Idea($scope.node.ideas);
});
