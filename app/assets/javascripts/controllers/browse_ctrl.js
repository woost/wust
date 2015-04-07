angular.module("wust").controller("BrowseCtrl", function($scope, $state, Problem, Goal, Idea, DiscourseNode) {
    var problems = {
        active: true,
        newTitle: "What is your problem?",
        listTitle: "Existing problems:",
        info: DiscourseNode.problem,
        queryNodes: _.wrap(Problem, queryNodes),
        addNode: addNode,
        newNode: Problem.$build({
            title: ""
        })
    };
    var goals = {
        newTitle: "What is your goal?",
        listTitle: "Existing goals:",
        info: DiscourseNode.goal,
        queryNodes: _.wrap(Goal, queryNodes),
        addNode: addNode,
        newNode: Goal.$build({
            title: ""
        })
    };
    var ideas = {
        newTitle: "What is your idea?",
        listTitle: "Existing ideas:",
        info: DiscourseNode.idea,
        queryNodes: _.wrap(Idea, queryNodes), // listing existing nodes
        addNode: addNode,
        newNode: Idea.$build({
            title: ""
        })
    };

    var slides = [problems, goals, ideas];
    $scope.slides = slides;

    $scope.$watch(() => _.find(slides, "active"), currentSlide => currentSlide.queryNodes());

    function addNode() {
        this.newNode.$save().$then(data => {
            humane.success("Added new node");
            $state.go(this.info.state, {
                id: data.id
            });
        });
    }

    function queryNodes(service) {
        service.$search().$then(data => this.nodes = data);
    }
});
