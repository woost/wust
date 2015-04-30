angular.module("wust.components").controller("UserListsCtrl", function($scope, User) {
    $scope.users = User.$search();
});
