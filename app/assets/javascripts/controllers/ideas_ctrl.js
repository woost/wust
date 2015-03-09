app.controller('IdeasCtrl', function($scope, $stateParams, Idea, Graph, DiscourseNodeList, DiscourseNode) {
    var id = $stateParams.id;

    $scope.nodeCss = DiscourseNode.idea.css;
    $scope.node = Idea.get(id);
    $scope.goals = new DiscourseNodeList.Goal(id, Idea.queryGoals, Idea.createGoal, Graph.remove);
    $scope.problems = new DiscourseNodeList.Problem(id, Idea.queryProblems, Idea.createProblem, Graph.remove);
    $scope.ideas = new DiscourseNodeList.Idea();
});
