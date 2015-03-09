app.controller('GoalsCtrl', function($scope, $stateParams, Goal, Graph, ItemList) {
    var id = $stateParams.id;

    $scope.nodeCss = ItemList.Goal.css;
    $scope.node = Goal.get(id);
    $scope.goals = new ItemList.Goal();
    $scope.problems = new ItemList.Problem(id, Goal.queryProblems, Goal.createProblem, Graph.remove);
    $scope.ideas = new ItemList.Idea(id, Goal.queryIdeas, Goal.createIdea, Graph.remove);
});
