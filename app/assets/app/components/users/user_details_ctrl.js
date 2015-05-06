angular.module("wust.components").controller("UserDetailsCtrl", UserDetailsCtrl);

UserDetailsCtrl.$inject = ["$stateParams", "User"];

function UserDetailsCtrl($stateParams, User) {
    let vm = this;

    vm.user = User.$find($stateParams.id);
}
