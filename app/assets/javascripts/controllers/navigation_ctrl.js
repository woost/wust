angular.module("wust").controller("NavigationCtrl", function($scope, Auth, Search, DiscourseNode, $state) {
    $scope.searchTyped = {
        title: ""
    };

    $scope.newUser = {
        identifier: "",
        password: ""
    };

    $scope.searchNodes = searchNodes;
    $scope.onSelect = onSelect;
    $scope.login = Auth.login.bind(Auth, $scope.newUser);
    $scope.register = Auth.register.bind(Auth, $scope.newUser);
    $scope.loggedIn = Auth.loggedIn.bind(Auth);
    $scope.logout = Auth.logout.bind(Auth);

    function searchNodes(title) {
        return Search.$search({title: title});
    }

    function onSelect($item) {
        let state = DiscourseNode.get($item.label).state;
        $state.go(state, {
            id: $item.id
        });
        $scope.searchTyped.title = "";
    }
});
