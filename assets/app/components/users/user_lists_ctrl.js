angular.module("wust.components").controller("UserListsCtrl", UserListsCtrl);

UserListsCtrl.$inject = ["$scope", "User", "ContextService"];

function UserListsCtrl($scope, User, ContextService) {
    let vm = this;

    let size = 20;
    let page = 0;
    vm.loadMore = loadMore;
    vm.users = User.$search(searchParams());

    $scope.$on("context.changed", () => {
        vm.users.$refresh(searchParams());
    });

    function loadMore() {
        page++;
        return vm.users.$fetch(searchParams());
    }

    function searchParams() {
        return {
            scopes: ContextService.currentContexts.map(c => c.id),
            page, size
        };
    }
}
