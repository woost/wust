angular.module("wust.components").controller("FooterCtrl", FooterCtrl);

FooterCtrl.$inject = ["$scope", "NodeHistory"];

function FooterCtrl($scope, NodeHistory) {
    $scope.visited = NodeHistory.visited;
}
