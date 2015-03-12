app.controller('IdeasCtrl', function($scope, Idea, DiscourseNodeView, DiscourseNode) {
    DiscourseNodeView($scope, DiscourseNode.idea, Idea);
});
