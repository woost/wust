app.controller("ProblemsCtrl", function($scope, Problem) {
    $scope.addProblem = addProblem;
    $scope.newProblem = {
        title: ""
    };

    Problem.get().$promise.then(function (data) {
        $scope.problems = data;
    }, function (response) {
        toastr.error("Failed to get problems");
    });

    function addProblem() {
        Problem.create($scope.newProblem).$promise.then(function(data) {
            $scope.problems.push(data);
            $scope.newProblem.title = "";
        }, function(response) {
            toastr.error("Cannot create new problem");
        });
    }
});
