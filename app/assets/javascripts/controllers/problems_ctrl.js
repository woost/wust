app.controller('ProblemsCtrl', function($scope, $stateParams, Problem, ItemList) {
    $scope.node = Problem.get($stateParams.id);
    $scope.goals = new ItemList.Item(Problem.queryGoals, Problem.createGoal);
    $scope.problems = new ItemList.Item();
    $scope.ideas = new ItemList.Item(Problem.queryIdeas, Problem.createIdea);
});
