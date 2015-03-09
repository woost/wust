app.controller("HomeCtrl", function($scope, Problem, Goal, Idea, DiscourseNode) {
    //TODO: only get nodes from server if actually displayed
    var problems = {
        active: true,
        state: "problems",
        newTitle: "What is your problem?",
        listTitle: "Existing problems:",
        css: DiscourseNode.problem.css,
        nodes: Problem.query(),
        newNode: {
            title: ""
        }
    };
    var goals = {
        state: "goals",
        newTitle: "What is your goal?",
        listTitle: "Existing goals:",
        css: DiscourseNode.goal.css,
        nodes: Goal.query(),
        newNode: {
            title: ""
        }
    };
    var ideas = {
        state: "ideas",
        newTitle: "What is your idea?",
        listTitle: "Existing ideas:",
        css: DiscourseNode.idea.css,
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
                container.nodes.push(data);
                container.newNode.title = "";
                toastr.success("Added new node");
            });
        };
    }
});
