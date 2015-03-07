app.controller('ProblemsCtrl', function($scope, $stateParams, Problem) {
    $scope.problem = Problem.get($stateParams.id);
    $scope.connected = {
        idea: {
            new: {
                title: ""
            },
            list: Problem.queryIdeas($stateParams.id),
            add: functionToDo,
            remove: functionToDo
        },
        goal: {
            new: {
                title: ""
            },
            list: Problem.queryGoals($stateParams.id),
            add: functionToDo,
            remove: functionToDo
        }
    };

    function functionToDo(something) {
        toastr.error("Hi, I am a function to be implemented");
    }
});
