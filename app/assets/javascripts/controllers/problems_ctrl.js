app.controller('ProblemsCtrl', function($scope, $stateParams, Problem, Graph, DiscourseNodeList, DiscourseNode) {
    var id = $stateParams.id;

    $scope.nodeCss = DiscourseNode.problem.css;
    $scope.node = Problem.get(id);
    $scope.goals = new DiscourseNodeList.Goal(id, Problem.queryGoals, Problem.createGoal, Graph.remove);
    $scope.problems = new DiscourseNodeList.Problem(id, Problem.queryProblems, Problem.createProblem, Graph.remove);
    $scope.ideas = new DiscourseNodeList.Idea(id, Problem.queryIdeas, Problem.createIdea, Graph.remove);
});
