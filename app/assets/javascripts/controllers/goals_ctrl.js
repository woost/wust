app.controller('GoalsCtrl', function($scope, $stateParams, Goal, ItemList) {
    $scope.node = Goal.get($stateParams.id);
    $scope.goals = new ItemList.Item("goals");
    $scope.problems = new ItemList.Item("problems");
    $scope.ideas = new ItemList.Item("ideas");
});
