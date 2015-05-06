angular.module("wust.components").controller("UserListsCtrl", UserListsCtrl);

UserListsCtrl.$inject = ["$scope", "User"];

function UserListsCtrl($scope, User) {
    $scope.users = User.$search();
}
