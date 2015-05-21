angular.module("wust.components").controller("UserDetailsCtrl", UserDetailsCtrl);

UserDetailsCtrl.$inject = ["$rootScope", "$stateParams", "User", "DiscourseNodeList"];

function UserDetailsCtrl($rootScope, $stateParams, User, DiscourseNodeList) {
    let vm = this;

    vm.user = User.$find($stateParams.id);

    vm.contributions = DiscourseNodeList.Any(vm.user.contributes.$search(), "Contributions");
    vm.contributions.subscribe();
}
