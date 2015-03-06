app.controller('ProblemsCtrl', function($scope, $stateParams, Problem) {
    $scope.selected = {
        problem: Problem.get($stateParams.id),
        ideas: Problem.queryIdeas($stateParams.id),
        goals: Problem.queryGoals($stateParams.id)
    };

    $scope.newIdea = {
        title: ""
    };

    $scope.newGoal = {
        title: ""
    };

    $scope.addIdea = todo;
    $scope.removeIdea = todo;
    $scope.addGoal = todo;
    $scope.removeGoal = todo;

    function todo() {
        toastr.error("Hi, I am a function to be implemented");
    }
});
