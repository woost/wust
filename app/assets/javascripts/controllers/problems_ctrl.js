app.controller('ProblemsCtrl', function($scope, $stateParams, Problem, DiscourseNodeList, DiscourseNodeView, DiscourseNode) {
    var id = $stateParams.id;

    $scope.nodeCss = DiscourseNode.problem.css;
    $scope.node = Problem.get(id);
    $scope.goals = new DiscourseNodeList.Goal(id, Problem);
    $scope.problems = new DiscourseNodeList.Problem(id, Problem);
    $scope.ideas = new DiscourseNodeList.Idea(id, Problem);
    $scope.removeFocused = DiscourseNodeView.removeFocused(Problem.remove, id);
});
