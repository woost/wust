app.controller('ProblemsCtrl', function($scope, $stateParams, Problem) {
    $scope.problem = Problem.get($stateParams.id);
});
