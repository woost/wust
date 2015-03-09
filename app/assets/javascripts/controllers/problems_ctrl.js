app.controller('ProblemsCtrl', function($scope, $stateParams, Problem, Graph, ItemList) {
    var id = $stateParams.id;

    $scope.nodeCss = ItemList.Problem.css;
    $scope.node = Problem.get(id);
    $scope.goals = new ItemList.Goal(id, Problem.queryGoals, Problem.createGoal, Graph.remove);
    $scope.problems = new ItemList.Problem(id, Problem.queryProblems, Problem.createProblem, Graph.remove);
    $scope.ideas = new ItemList.Idea(id, Problem.queryIdeas, Problem.createIdea, Graph.remove);
});
