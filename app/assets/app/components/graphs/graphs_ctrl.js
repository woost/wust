angular.module("wust.components").controller("GraphsCtrl", GraphsCtrl);

GraphsCtrl.$inject = ["$scope", "$filter", "Graph", "DiscourseNode"];

function GraphsCtrl($scope, $filter, Graph, DiscourseNode) {
    let vm = this;

    vm.graph = Graph.$fetch();
}
