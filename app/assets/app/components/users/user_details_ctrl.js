angular.module("wust.components").controller("UserDetailsCtrl", UserDetailsCtrl);

UserDetailsCtrl.$inject = ["$scope", "$stateParams", "User"];

function UserDetailsCtrl($scope, $stateParams, User) {
    $scope.user = User.$find($stateParams.id);
}
