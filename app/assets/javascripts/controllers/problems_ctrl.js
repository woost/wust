app.controller('ProblemsCtrl', function($scope, $stateParams, Problem, ItemList) {
    $scope.node = Problem.get($stateParams.id);
    $scope.goals = new ItemList.Item("goals", Problem.queryGoals, Problem.createGoal);
    $scope.problems = new ItemList.Item("problems");
    $scope.ideas = new ItemList.Item("ideas", Problem.queryIdeas, Problem.createIdea);
});
