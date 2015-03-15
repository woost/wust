angular.module("wust").controller("IdeasCtrl", function($scope, Idea, DiscourseNodeView, DiscourseNode) {
    DiscourseNodeView.setScope($scope, DiscourseNode.idea, Idea);
});
