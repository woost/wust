angular.module("wust.components").controller("UserDetailsCtrl", UserDetailsCtrl);

UserDetailsCtrl.$inject = ["$stateParams", "User", "DiscourseNodeList"];

function UserDetailsCtrl($stateParams, User, DiscourseNodeList) {
    let vm = this;

    vm.user = User.$find($stateParams.id);

    vm.contributions = DiscourseNodeList.read(vm.user.contributes.$search(), "Contributions");
    vm.contributions.subscribe();
}
