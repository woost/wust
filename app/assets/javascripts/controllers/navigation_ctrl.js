angular.module("wust").controller("NavigationCtrl", function($scope, Auth, Search, DiscourseNode, $state) {
    $scope.searchTyped = {
        title: ""
    };

    $scope.registerUser = false;
    $scope.newUser = {
        identifier: "",
        password: ""
    };

    $scope.searchNodes = searchNodes;
    $scope.onSelect = onSelect;
    $scope.authenticate = authenticate;
    $scope.getUsername = Auth.getUsername.bind(Auth);
    $scope.loggedIn = Auth.loggedIn.bind(Auth);
    $scope.logout = Auth.logout.bind(Auth);

    function authenticate() {
        let func = $scope.registerUser ? Auth.register : Auth.login;
        func.bind(Auth, angular.copy($scope.newUser))();
        $scope.newUser.identifier = "";
        $scope.newUser.password = "";
    }

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
