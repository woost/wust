angular.module("wust").controller("FooterCtrl", function($scope, NodeHistory) {
    $scope.visited = NodeHistory.visited;
});
