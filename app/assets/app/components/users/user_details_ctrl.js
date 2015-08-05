angular.module("wust.components").controller("UserDetailsCtrl", UserDetailsCtrl);

UserDetailsCtrl.$inject = ["$stateParams", "User", "Auth", "$q"];

function UserDetailsCtrl($stateParams, User, Auth, $q) {
    let vm = this;

    vm.user = User.$find($stateParams.id);
    vm.isCurrentUser = $stateParams.id === Auth.current.userId;
    vm.saveUser = saveUser;

    let page = 0;
    vm.contributions = vm.user.contributions.$search({page});
    vm.loadMore = loadMore;
    vm.noMore = false;

    function saveUser() {
        return vm.user.$save().$then(() => {
            humane.success("Updated user profile");
        }, () => humane.error("Error updating user profile")).$asPromise();
    }

    function loadMore() {
        if (vm.noMore)
            return;

        page++;
        let prevLength = vm.contributions.length;
        vm.contributions.$fetch({page}).$then(val => {
            vm.noMore = val.length === prevLength;
        });
    }
}
