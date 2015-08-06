angular.module("wust.components").controller("UserListsCtrl", UserListsCtrl);

UserListsCtrl.$inject = ["User"];

function UserListsCtrl(User) {
    let vm = this;

    let size = 20;
    let page = 0;
    vm.loadMore = loadMore;
    vm.users = User.$search({page, size});

    function loadMore() {
        page++;
        return vm.users.$fetch({page, size});
    }
}
