angular.module("wust").controller("GoalsCtrl", function($scope, Goal, DiscourseNodeView, DiscourseNode) {
    DiscourseNodeView.setScope($scope, DiscourseNode.goal, Goal);
});
