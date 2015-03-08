app.controller('GoalsCtrl', function($scope, $stateParams, Goal, ItemList) {
    $scope.node = Goal.get($stateParams.id);
    $scope.goals = new ItemList.Item();
    $scope.problems = new ItemList.Item(Goal.queryProblems, Goal.createProblem);
    $scope.ideas = new ItemList.Item(Goal.queryIdeas, Goal.createIdea);
});
