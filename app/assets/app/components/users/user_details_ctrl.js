angular.module("wust.components").controller("UserDetailsCtrl", function($scope, $stateParams, User) {
    $scope.user = User.$find($stateParams.id);
});
