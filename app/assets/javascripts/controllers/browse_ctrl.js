angular.module("wust").controller("BrowseCtrl", function($scope, $state, Problem, Goal, Idea, DiscourseNode) {
    var problems = {
        active: true,
        newTitle: "What is your problem?",
        listTitle: "Existing problems:",
        info: DiscourseNode.problem,
        service: Problem,
        addNode: addNode,
        newNode: Problem.$build({
            title: ""
        })
    };
    var goals = {
        newTitle: "What is your goal?",
        listTitle: "Existing goals:",
        info: DiscourseNode.goal,
        service: Goal,
        addNode: addNode,
        newNode: Goal.$build({
            title: ""
        })
    };
    var ideas = {
        newTitle: "What is your idea?",
        listTitle: "Existing ideas:",
        info: DiscourseNode.idea,
        service: Idea,
        addNode: addNode,
        newNode: Idea.$build({
            title: ""
        })
    };

    $scope.slides = [problems, goals, ideas];

    $scope.$watch(() => _.find($scope.slides, "active"), active => setNodes(active));

    function addNode() {
        this.newNode.$save().$then(data => {
            humane.success("Added new node");
            $state.go(this.info.state, {
                id: data.id
            });
        });
    }

    function setNodes(slide) {
        slide.nodes = slide.service.$search();
    }
});
