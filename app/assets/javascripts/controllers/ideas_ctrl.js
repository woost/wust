app.controller('IdeasCtrl', function($scope, $stateParams, Idea, ItemList) {
    $scope.node = Idea.get($stateParams.id);
    $scope.goals = new ItemList.Item("goals");
    $scope.problems = new ItemList.Item("problems");
    $scope.ideas = new ItemList.Item("ideas");
});
