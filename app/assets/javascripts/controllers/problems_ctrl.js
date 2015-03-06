app.controller('ProblemsCtrl', function($scope, $stateParams, Problem) {
    $scope.selected = {
        problem: Problem.get($stateParams.id),
        ideas: Problem.queryIdeas($stateParams.id),
        goals: Problem.queryGoals($stateParams.id)
    };
});
