angular.module("wust.components").controller("ProblemsCtrl", function($scope, $stateParams, Problem, DiscourseNode, DiscourseNodeList) {
    $scope.nodeInfo = DiscourseNode.Problem;
    $scope.node = Problem.$find($stateParams.id);
    $scope.top = new DiscourseNodeList.Goal($scope.node.goals);
    $scope.left = new DiscourseNodeList.Problem($scope.node.causes, "Causes");
    $scope.right = new DiscourseNodeList.Problem($scope.node.consequences, "Consequences");
    $scope.bottom = new DiscourseNodeList.Idea($scope.node.ideas)
        .nested(DiscourseNodeList.ProArgument, "pros")
        .nested(DiscourseNodeList.ConArgument, "cons");
});
