app.controller("BrowseCtrl", function($scope, $state, Problem, Goal, Idea, DiscourseNode) {
    //TODO: only get nodes from server if actually displayed
    var problems = {
        active: true,
        newTitle: "What is your problem?",
        listTitle: "Existing problems:",
        info: DiscourseNode.problem,
        queryNodes: queryNodes(Problem.query),
        addNode: addNode(Problem.create),
        newNode: {
            title: ""
        }
    };
    var goals = {
        newTitle: "What is your goal?",
        listTitle: "Existing goals:",
        info: DiscourseNode.goal,
        queryNodes: queryNodes(Goal.query),
        addNode: addNode(Goal.create),
        newNode: {
            title: ""
        }
    };
    var ideas = {
        newTitle: "What is your idea?",
        listTitle: "Existing ideas:",
        info: DiscourseNode.idea,
        queryNodes: queryNodes(Idea.query),
        addNode: addNode(Idea.create),
        newNode: {
            title: ""
        }
    };

    var slides = [problems, goals, ideas];
    $scope.slides = slides;

    $scope.$watch(function() {
        for (var i = 0; i < slides.length; i++) {
            if (slides[i].active) {
                return slides[i];
            }
        }
    }, function(currentSlide, previousSlide) {
        currentSlide.queryNodes();
    });

    function addNode(createFunc) {
        return function() {
            var self = this;
            createFunc(self.newNode).$promise.then(function(data) {
                toastr.success("Added new node");
                $state.go(self.info.state, {
                    id: data.id
                });
            });
        };
    }

    function queryNodes(queryFunc) {
        return function() {
            var self = this;
            queryFunc().$promise.then(function(data) {
                self.nodes = data;
            });
        };
    }
});
