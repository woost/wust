app.controller('GoalsCtrl', function($scope, $stateParams, Goal, DiscourseNodeList, DiscourseNodeView, DiscourseNode) {
    var id = $stateParams.id;

    $scope.nodeCss = DiscourseNode.goal.css;
    $scope.node = Goal.get(id);
    $scope.goals = new DiscourseNodeList.Goal();
    $scope.problems = new DiscourseNodeList.Problem(id, Goal);
    $scope.ideas = new DiscourseNodeList.Idea(id, Goal);
    $scope.removeFocused = DiscourseNodeView.removeFocused(Goal.remove, id);
});
