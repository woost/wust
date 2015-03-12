app.controller('IdeasCtrl', function($scope, $stateParams, Idea, DiscourseNodeList, DiscourseNodeView, DiscourseNode) {
    var id = $stateParams.id;

    $scope.nodeCss = DiscourseNode.idea.css;
    $scope.node = Idea.get(id);
    $scope.goals = new DiscourseNodeList.Goal(id, Idea);
    $scope.problems = new DiscourseNodeList.Problem(id, Idea);
    $scope.ideas = new DiscourseNodeList.Idea();
    $scope.removeFocused = DiscourseNodeView.removeFocused(Idea.remove, id);
});
