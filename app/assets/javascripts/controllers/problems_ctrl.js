app.controller('ProblemsCtrl', function($scope, $stateParams, Problem, DiscourseNodeList, DiscourseNode) {
    var id = $stateParams.id;

    $scope.nodeCss = DiscourseNode.problem.css;
    $scope.node = Problem.get(id);
    $scope.goals = new DiscourseNodeList.Goal(id, Problem.queryGoals, Problem.createGoal, Problem.removeGoal);
    $scope.problems = new DiscourseNodeList.Problem(id, Problem.queryProblems, Problem.createProblem, Problem.removeProblem);
    $scope.ideas = new DiscourseNodeList.Idea(id, Problem.queryIdeas, Problem.createIdea);
});
