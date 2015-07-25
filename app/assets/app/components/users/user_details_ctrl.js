angular.module("wust.components").controller("UserDetailsCtrl", UserDetailsCtrl);

UserDetailsCtrl.$inject = ["$stateParams", "User", "Auth", "$q"];

function UserDetailsCtrl($stateParams, User, Auth, $q) {
    let vm = this;

    vm.user = User.$find($stateParams.id);
    vm.isCurrentUser = $stateParams.id === Auth.getUserId();
    vm.saveUser = saveUser;

    vm.contributions = [];
    let created = vm.user.created.$search().$asPromise();
    let updated = vm.user.updated.$search().$asPromise();
    let deleted = vm.user.deleted.$search().$asPromise();
    $q.all([created, updated, deleted]).then(list => vm.contributions = _([{
        title: "Created",
        data: list[0]
    }, {
        title: "Updated",
        data: list[1]
    }, {
        title: "Deleted",
        data: list[2]
    }]).map(c => _.map(c.data, d => {
        return {
            title: c.title,
            data: d
        };
    })).flatten().value());

    function saveUser() {
        return vm.user.$save().$asPromise();
    }
}
