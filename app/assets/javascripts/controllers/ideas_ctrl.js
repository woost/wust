app.controller('IdeasCtrl', function($scope, $stateParams, Idea, ItemList) {
    $scope.node = Idea.get($stateParams.id);
    $scope.goals = new ItemList.Item(Idea.queryGoals, Idea.createGoal);
    $scope.problems = new ItemList.Item(Idea.queryProblems, Idea.createProblem);
    $scope.ideas = new ItemList.Item();
});
