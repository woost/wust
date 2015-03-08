app.controller('IdeasCtrl', function($scope, $stateParams, Idea, ItemList) {
    $scope.node = Idea.get($stateParams.id);
    $scope.goals = new ItemList.Item();
    $scope.problems = new ItemList.Item();
    $scope.ideas = new ItemList.Item();
});
