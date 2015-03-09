app.controller('GoalsCtrl', function($scope, $stateParams, Goal, Graph, DiscourseNodeList, DiscourseNode) {
    var id = $stateParams.id;

    $scope.nodeCss = DiscourseNode.goal.css;
    $scope.node = Goal.get(id);
    $scope.goals = new DiscourseNodeList.Goal();
    $scope.problems = new DiscourseNodeList.Problem(id, Goal.queryProblems, Goal.createProblem, Graph.remove);
    $scope.ideas = new DiscourseNodeList.Idea(id, Goal.queryIdeas, Goal.createIdea, Graph.remove);
});
