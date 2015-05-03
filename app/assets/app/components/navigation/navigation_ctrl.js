angular.module("wust.components").controller("NavigationCtrl", function($scope, Auth, Search, DiscourseNode) {
    $scope.searchTyped = {
        title: ""
    };

    $scope.newUser = {
        identifier: "",
        password: ""
    };

    $scope.searchNodes = searchNodes;
    $scope.onSelect = onSelect;
    $scope.onSubmit = onSubmit;
    $scope.authenticate = authenticate;
    $scope.getUsername = Auth.getUsername.bind(Auth);
    $scope.loggedIn = Auth.loggedIn.bind(Auth);
    $scope.logout = Auth.logout.bind(Auth);

    function authenticate(register) {
        let func = register ? Auth.register : Auth.login;
        func.bind(Auth, angular.copy($scope.newUser))();
        $scope.newUser.identifier = "";
        $scope.newUser.password = "";
    }

    // provides a promise of the searchresults for the auto completion
    function searchNodes(title) {
        return Search.$search({title: title});
    }

    // focus the first node of the search results
    function onSubmit(nodes) {
        if (_.isEmpty(nodes)) {
            humane.error("Nothing found");
            return;
        }

        focusNode(nodes[0]);
    }

    // focus the selected node
    function onSelect(item) {
        focusNode(item);
        $scope.searchTyped.title = "";
    }

    // route to the node's page
    function focusNode(node) {
        DiscourseNode.get(node.label).gotoState(node.id);
    }
});
