angular.module("wust").controller("GoalsCtrl", function($scope, Goal, DiscourseNodeView, DiscourseNode) {
    DiscourseNodeView($scope, DiscourseNode.Goal, Goal);
});
