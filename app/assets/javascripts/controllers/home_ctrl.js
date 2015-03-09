app.controller("HomeCtrl", function($scope, $state, Problem, Goal, Idea, DiscourseNode) {
    //TODO: only get nodes from server if actually displayed
    var problems = {
        active: true,
        newTitle: "What is your problem?",
        listTitle: "Existing problems:",
        info: DiscourseNode.problem,
        nodes: Problem.query(),
        newNode: {
            title: ""
        }
    };
    var goals = {
        newTitle: "What is your goal?",
        listTitle: "Existing goals:",
        info: DiscourseNode.goal,
        nodes: Goal.query(),
        newNode: {
            title: ""
        }
    };
    var ideas = {
        newTitle: "What is your idea?",
        listTitle: "Existing ideas:",
        info: DiscourseNode.idea,
        nodes: Idea.query(),
        newNode: {
            title: ""
        }
    };

    problems.addNode = addNode(Problem.create, problems);
    goals.addNode = addNode(Goal.create, goals);
    ideas.addNode = addNode(Idea.create, ideas);

    $scope.slides = [problems, goals, ideas];

    function addNode(createFunc, container) {
        return function() {
            createFunc(container.newNode).$promise.then(function(data) {
                $state.go(container.info.state, { id: data.id });
                toastr.success("Added new node");
            });
        };
    }
});
