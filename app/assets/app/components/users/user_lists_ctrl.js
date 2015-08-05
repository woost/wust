angular.module("wust.components").controller("UserListsCtrl", UserListsCtrl);

UserListsCtrl.$inject = ["User"];

function UserListsCtrl(User) {
    let vm = this;

    let size = 20;
    let page = 0;
    vm.loadMore = loadMore;
    vm.noMore = false;
    vm.users = User.$search({page, size}).$then(val => {
        vm.noMore = val.length < size;
    });

    function loadMore() {
        if (vm.noMore)
            return;

        page++;
        let prevLength = vm.users.length;
        vm.users.$fetch({page}).$then(val => {
            let diff = val.length - prevLength;
            vm.noMore = diff < size;
        });
    }
}
