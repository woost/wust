angular.module("wust.components").controller("IdeasCtrl", function($scope, $stateParams, Idea, DiscourseNode, DiscourseNodeList) {
    $scope.nodeInfo = DiscourseNode.Idea;
    $scope.node = Idea.$find($stateParams.id);
    $scope.top = new DiscourseNodeList.Goal($scope.node.goals)
        .nested(DiscourseNodeList.ProArgument, "pros")
        .nested(DiscourseNodeList.ConArgument, "cons");
    $scope.left = new DiscourseNodeList.Problem($scope.node.problems)
        .nested(DiscourseNodeList.ProArgument, "pros")
        .nested(DiscourseNodeList.ConArgument, "cons");
    $scope.bottom = new DiscourseNodeList.Idea($scope.node.ideas);
});
