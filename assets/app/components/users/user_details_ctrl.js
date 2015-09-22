angular.module("wust.components").controller("UserDetailsCtrl", UserDetailsCtrl);

UserDetailsCtrl.$inject = ["$stateParams", "User", "Auth", "$q"];

function UserDetailsCtrl($stateParams, User, Auth, $q) {
    let vm = this;

    vm.user = User.$find($stateParams.id);
    vm.isCurrentUser = $stateParams.id === Auth.current.userId;
    vm.saveUser = saveUser;
    vm.karmaTags = User.$buildRaw({id: $stateParams.id}).karma.$search();
    vm.karmaSum = 0;
    vm.karmaTags.$then(tags => {
        vm.karmaSum = tags.map(t => t.karma).reduce((a,b) => a+b);
    });

    let size = 20;
    let page = 0;
    vm.loadMore = loadMore;
    vm.contributions = vm.user.contributions.$search({page, size});

    function saveUser() {
        return vm.user.$save().$then(() => {
            humane.success("Updated user profile");
        }, () => humane.error("Error updating user profile")).$asPromise();
    }

    function loadMore() {
        page++;
        return vm.contributions.$fetch({page, size});
    }
}
