angular.module("wust.components").controller("UserDetailsCtrl", UserDetailsCtrl);

UserDetailsCtrl.$inject = ["$stateParams", "User", "Auth", "$q"];

function UserDetailsCtrl($stateParams, User, Auth, $q) {
    let vm = this;

    vm.authInfo = {};
    vm.user = User.$find($stateParams.id);
    vm.isCurrentUser = $stateParams.id === Auth.current.userId;
    vm.saveUser = saveUser;
    vm.savePassword = savePassword;
    vm.karmaTags = User.$buildRaw({
        id: $stateParams.id
    }).karmaContexts.$search();
    vm.karmaSum = 0;
    vm.karmaTags.$then(tags => {
        vm.karmaSum = tags.map(t => t.karma).reduce((a, b) => a + b, 0);
    });

    vm.karmaLog = User.$buildRaw({
        id: $stateParams.id
    }).karmaLog.$search();

    let size = 20;
    let page = 0;
    vm.loadMore = loadMore;
    vm.contributions = vm.user.contributions.$search({
        page,
        size
    });

    function saveUser() {
        return vm.user.$save().$then(() => {
            // humane.success("Updated user profile");
        }, () => humane.error("Error updating user profile")).$asPromise();
    }

    function savePassword() {
        if (!vm.authInfo.password1 || !vm.authInfo.password2)
            return;

        if (vm.authInfo.password1 !== vm.authInfo.password2) {
            humane.error("passwords do not match");
        }

        var user = User.$buildRaw(_.pick(vm.user, "id"));
        user.password = vm.authInfo.password1;
        return user.$save().$then(() => {
            vm.authInfo.password1 = "";
            vm.authInfo.password2 = "";
            humane.success("Updated user password");
        }, () => {
            humane.error("Error updating user password");
        }).$asPromise();
    }

    function loadMore() {
        page++;
        return vm.contributions.$fetch({
            page,
            size
        });
    }
}
