angular.module("wust.components").controller("FooterCtrl", function($scope, NodeHistory) {
    $scope.visited = NodeHistory.visited;
});
