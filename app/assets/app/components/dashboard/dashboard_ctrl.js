angular.module("wust.components").controller("DashboardCtrl", DashboardCtrl);

DashboardCtrl.$inject = ["$scope", "$state", "Post", "DiscourseNode"];

function DashboardCtrl($scope, $state, Post, DiscourseNode) {
    let vm = this;

    vm.stream = {
        info: DiscourseNode.Post,
        nodes: Post.$search()
    };
}
