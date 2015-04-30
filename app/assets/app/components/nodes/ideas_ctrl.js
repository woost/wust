angular.module("wust.components").controller("IdeasCtrl", function($scope, $stateParams, Idea, DiscourseNode, DiscourseNodeList) {
    $scope.nodeInfo = DiscourseNode.Idea;
    $scope.node = Idea.$find($stateParams.id);
    $scope.top = new DiscourseNodeList.Goal($scope.node.goals);
    $scope.left = new DiscourseNodeList.Problem($scope.node.problems);
    $scope.bottom = new DiscourseNodeList.Idea($scope.node.ideas);
});
