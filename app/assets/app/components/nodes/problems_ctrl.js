angular.module("wust.components").controller("ProblemsCtrl", ProblemsCtrl);

ProblemsCtrl.$inject = ["$scope", "$stateParams", "Problem", "DiscourseNode", "DiscourseNodeList"];

function ProblemsCtrl($scope, $stateParams, Problem, DiscourseNode, DiscourseNodeList) {
    $scope.nodeInfo = DiscourseNode.Problem;
    $scope.node = Problem.$find($stateParams.id);
    $scope.top = DiscourseNodeList.Goal($scope.node.goals);
    $scope.left = DiscourseNodeList.Problem($scope.node.causes, "Causes");
    $scope.right = DiscourseNodeList.Problem($scope.node.consequences, "Consequences");
    $scope.bottom = DiscourseNodeList.Idea($scope.node.ideas)
        .nested(DiscourseNodeList.ProArgument, "pros")
        .nested(DiscourseNodeList.ConArgument, "cons");
}
