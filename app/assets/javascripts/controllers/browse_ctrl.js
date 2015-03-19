angular.module("wust").controller("BrowseCtrl", function($scope, $state, Problem, Goal, Idea, DiscourseNode) {
    var problems = {
        active: true,
        newTitle: "What is your problem?",
        listTitle: "Existing problems:",
        info: DiscourseNode.problem,
        queryNodes: _.wrap(Problem.query, queryNodes),
        addNode: _.wrap(Problem.create, addNode),
        newNode: {
            title: ""
        }
    };
    var goals = {
        newTitle: "What is your goal?",
        listTitle: "Existing goals:",
        info: DiscourseNode.goal,
        queryNodes: _.wrap(Goal.query, queryNodes),
        addNode: _.wrap(Goal.create, addNode),
        newNode: {
            title: ""
        }
    };
    var ideas = {
        newTitle: "What is your idea?",
        listTitle: "Existing ideas:",
        info: DiscourseNode.idea,
        queryNodes: _.wrap(Idea.query, queryNodes), // listing existing nodes
        addNode: _.wrap(Idea.create, addNode), // adding new nodes
        newNode: {
            title: ""
        }
    };

    var slides = [problems, goals, ideas];
    $scope.slides = slides;

    $scope.$watch(() => _.find(slides, "active"), currentSlide => currentSlide.queryNodes());

    function addNode(createFunc) {
        createFunc(this.newNode).$promise.then(data => {
            humane.success("Added new node");
            $state.go(this.info.state, {
                id: data.id
            });
        });
    }

    function queryNodes(queryFunc) {
        queryFunc().$promise.then(data => this.nodes = data);
    }
});
