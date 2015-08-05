angular.module("wust.components").controller("UserListsCtrl", UserListsCtrl);

UserListsCtrl.$inject = ["User"];

function UserListsCtrl(User) {
    let vm = this;

    let page = 0;
    vm.users = User.$search({page});
    vm.loadMore = loadMore;
    vm.noMore = false;

    function loadMore() {
        if (vm.noMore)
            return;

        page++;
        let prevLength = vm.users.length;
        vm.users.$fetch({page}).$then(val => {
            vm.noMore = val.length === prevLength;
        });
    }
}
