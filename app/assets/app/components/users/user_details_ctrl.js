angular.module("wust.components").controller("UserDetailsCtrl", UserDetailsCtrl);

UserDetailsCtrl.$inject = ["$stateParams", "User", "DiscourseNodeList"];

function UserDetailsCtrl($stateParams, User, DiscourseNodeList) {
    let vm = this;

    vm.user = User.$find($stateParams.id);

    vm.contributions = DiscourseNodeList.Any(vm.user.contributes, "Contributions");
}
