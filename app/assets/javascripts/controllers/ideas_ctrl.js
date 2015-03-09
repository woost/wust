app.controller('IdeasCtrl', function($scope, $stateParams, Idea, ItemList) {
    var id = $stateParams.id;

    $scope.nodeCss = ItemList.Idea.css;
    $scope.node = Idea.get(id);
    $scope.goals = new ItemList.Goal(id, Idea.queryGoals, Idea.createGoal);
    $scope.problems = new ItemList.Problem(id, Idea.queryProblems, Idea.createProblem);
    $scope.ideas = new ItemList.Idea();
});
