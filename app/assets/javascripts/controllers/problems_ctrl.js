angular.module("wust").controller("ProblemsCtrl", function($scope, Problem, DiscourseNodeView, DiscourseNode) {
    DiscourseNodeView($scope, DiscourseNode.Problem, Problem);
});
