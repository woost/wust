angular.module("wust").controller("NavigationCtrl", function($scope, $auth, Search, DiscourseNode, $state) {
    $scope.searchTyped = {
        title: ""
    };

    $scope.newUser = {
        identifier: "",
        password: ""
    };

    $scope.searchNodes = searchNodes;
    $scope.onSelect = onSelect;
    $scope.authenticate = authenticate;
    $scope.oauth = oauth;
    $scope.logout = logout;
    $scope.getUsername = $auth.getPayload.bind($auth);
    $scope.loggedIn = $auth.isAuthenticated.bind($auth);

    function authenticate(register) {
        let [message, func] = register ? ["Registered", $auth.signup] : ["Logged in", $auth.login];
        func.bind($auth)(angular.copy($scope.newUser)).then(() => humane.success(message));
        $scope.newUser.identifier = "";
        $scope.newUser.password = "";
    }

    function logout() {
        $auth.logout().then(() => humane.success("Logged out"));
    }

    function oauth(provider) {
        $auth.authenticate(provider).then(() => humane.success("Logged in with provider"));
        $scope.newUser.identifier = "";
        $scope.newUser.password = "";
    }

    function searchNodes(title) {
        return Search.$search({title: title});
    }

    function onSelect(item) {
        let state = DiscourseNode.get(item.label).state;
        $state.go(state, {
            id: item.id
        });
        $scope.searchTyped.title = "";
    }
});
