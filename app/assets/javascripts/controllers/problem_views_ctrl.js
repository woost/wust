app.controller('ProblemViewsCtrl', function($scope, $state, $stateParams, Problem) {
    $scope.ideas = Problem.queryIdeas($stateParams.id);

    $scope.addIdea = addIdea;
    $scope.removeIdea = undefined;
    $scope.newIdea = {
        title: ""
    };

    function addIdea() {
        Problem.createIdea($stateParams.id, $scope.newIdea).$promise.then(function(data) {
            toastr.success("Added new idea");
            $scope.ideas.push(data);
            $scope.newIdea.title = "";
        });
    }
});
