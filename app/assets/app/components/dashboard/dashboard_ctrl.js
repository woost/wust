angular.module("wust.components").controller("DashboardCtrl", DashboardCtrl);

DashboardCtrl.$inject = ["$scope", "$state", "Post", "DiscourseNode"];

function DashboardCtrl($scope, $state, Post, DiscourseNode) {
    let vm = this;

    vm.nodes = Post.$search();
}
