angular.module("wust").controller("ProblemsCtrl", function($scope, Problem, DiscourseNodeView, DiscourseNode) {
    DiscourseNodeView.setScope($scope, DiscourseNode.problem, Problem);
});
