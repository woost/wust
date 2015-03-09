app.controller('ProblemsCtrl', function($scope, $stateParams, Problem, ItemList) {
    var id = $stateParams.id;

    $scope.nodeCss = ItemList.Problem.css;
    $scope.node = Problem.get(id);
    $scope.goals = new ItemList.Goal(id, Problem.queryGoals, Problem.createGoal);
    $scope.problems = new ItemList.Problem(id, Problem.queryProblems, Problem.createProblem);
    $scope.ideas = new ItemList.Idea(id, Problem.queryIdeas, Problem.createIdea);
});
