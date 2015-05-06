angular.module("wust.components").controller("NavigationCtrl", NavigationCtrl);

NavigationCtrl.$inject = ["Auth", "Search", "DiscourseNode"];

function NavigationCtrl(Auth, Search, DiscourseNode) {
    let vm = this;

    vm.searchTyped = {
        title: ""
    };

    vm.newUser = {
        identifier: "",
        password: ""
    };

    vm.searchNodes = searchNodes;
    vm.onSelect = onSelect;
    vm.onSubmit = onSubmit;
    vm.authenticate = authenticate;
    vm.getUsername = Auth.getUsername.bind(Auth);
    vm.loggedIn = Auth.loggedIn.bind(Auth);
    vm.logout = Auth.logout.bind(Auth);

    function authenticate(register) {
        let func = register ? Auth.register : Auth.login;
        func.bind(Auth, angular.copy(vm.newUser))();
        vm.newUser.identifier = "";
        vm.newUser.password = "";
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
        vm.searchTyped.title = "";
    }

    // route to the node's page
    function focusNode(node) {
        DiscourseNode.get(node.label).gotoState(node.id);
    }
}
