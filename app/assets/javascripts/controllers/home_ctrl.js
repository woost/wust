app.controller("HomeCtrl", function($scope, Problem) {
    $scope.addProblem = addProblem;
    $scope.newProblem = {
        title: ""
    };

    $scope.problems = Problem.query();

    function addProblem() {
        Problem.create($scope.newProblem).$promise.then(function(data) {
            $scope.problems.push(data);
            $scope.newProblem.title = "";
            toastr.success("Added new Problem");
        });
    }
});
