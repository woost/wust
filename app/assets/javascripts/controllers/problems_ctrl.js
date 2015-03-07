app.controller('ProblemsCtrl', function($scope, $stateParams, Problem) {
    $scope.problem = Problem.get($stateParams.id);

    var newIdea = {
        title: ""
    };
    var ideas = Problem.queryIdeas($stateParams.id);
    $scope.ideas = {
        new: newIdea,
        list: ideas,
        add: addConnected(ideas, newIdea),
        remove: functionToDo
    };

    var newGoal = {
        title: ""
    };
    var goals = Problem.queryGoals($stateParams.id);
    $scope.goals = {
        new: newGoal,
        list: goals,
        add: addConnected(goals, newGoal),
        remove: functionToDo
    };

    function addConnected(itemList, newItem) {
        return function() {
            Problem.createIdea($stateParams.id, newItem).$promise.then(function(data) {
                toastr.success("Added new Item");
                itemList.push(data);
            });
        };
    }

    function functionToDo(something) {
        toastr.error("Hi, I am a function to be implemented");
    }
});
