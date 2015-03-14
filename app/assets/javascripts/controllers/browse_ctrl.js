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
        queryNodes: _.wrap(Idea.query, queryNodes),
        addNode: _.wrap(Idea.create, addNode),
        newNode: {
            title: ""
        }
    };

    var slides = [problems, goals, ideas];
    $scope.slides = slides;

    $scope.$watch(function() {
        return _.find(slides, "active");
    }, function(currentSlide, previousSlide) {
        currentSlide.queryNodes();
    });

    function addNode(createFunc) {
        var self = this;
        createFunc(self.newNode).$promise.then(function(data) {
            toastr.success("Added new node");
            $state.go(self.info.state, {
                id: data.id
            });
        });
    }

    function queryNodes(queryFunc) {
        var self = this;
        queryFunc().$promise.then(function(data) {
            self.nodes = data;
        });
    }
});
