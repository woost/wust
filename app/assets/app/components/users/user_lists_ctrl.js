angular.module("wust.components").controller("UserListsCtrl", UserListsCtrl);

UserListsCtrl.$inject = ["User"];

function UserListsCtrl(User) {
    let vm = this;

    vm.users = User.$search();
}
