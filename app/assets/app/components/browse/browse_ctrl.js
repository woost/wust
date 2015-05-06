angular.module("wust.components").controller("BrowseCtrl", BrowseCtrl);

BrowseCtrl.$inject = ["$scope", "$state", "Problem", "Goal", "Idea", "DiscourseNode"];

function BrowseCtrl($scope, $state, Problem, Goal, Idea, DiscourseNode) {
    let vm = this;

    let problems = {
        active: true,
        newTitle: "What is your problem?",
        listTitle: "Existing problems:",
        info: DiscourseNode.Problem,
        service: Problem,
        addNode: addNode,
        newNode: Problem.$build({
            title: ""
        })
    };
    let goals = {
        newTitle: "What is your goal?",
        listTitle: "Existing goals:",
        info: DiscourseNode.Goal,
        service: Goal,
        addNode: addNode,
        newNode: Goal.$build({
            title: ""
        })
    };
    let ideas = {
        newTitle: "What is your idea?",
        listTitle: "Existing ideas:",
        info: DiscourseNode.Idea,
        service: Idea,
        addNode: addNode,
        newNode: Idea.$build({
            title: ""
        })
    };

    vm.slides = [problems, goals, ideas];

    $scope.$watch(() => _.find(vm.slides, "active"), active => setNodes(active));

    function addNode() {
        angular.copy(this.newNode).$save().$then(data => {
            humane.success("Added new node");
            $state.go(this.info.state, {
                id: data.id
            });
        });
    }

    function setNodes(slide) {
        slide.nodes = slide.service.$search();
    }
}
