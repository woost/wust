app.controller('ProblemsCtrl', function($scope, $stateParams, Problem) {
    $scope.problem = Problem.get($stateParams.id);
    $scope.goals = Problem.queryGoals($stateParams.id);

    $scope.addGoal = addGoal;
    $scope.newGoal = {
        title: ""
    };

    function addGoal() {
        Problem.createGoal($stateParams.id, $scope.newGoal).$promise.then(function(data) {
            toastr.success("Added new goal");
            $scope.goals.push(data);
            $scope.newGoal.title = "";
        });
    }
});
