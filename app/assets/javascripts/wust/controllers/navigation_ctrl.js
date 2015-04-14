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
    $scope.authenticate = authenticate;
    $scope.oauth = Auth.oauth;
    $scope.getUsername = Auth.getUsername;
    $scope.loggedIn = Auth.loggedIn;
    $scope.logout = Auth.logout;

    function authenticate(register) {
        let func = register ? Auth.register : Auth.login;
        func(angular.copy($scope.newUser));
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
