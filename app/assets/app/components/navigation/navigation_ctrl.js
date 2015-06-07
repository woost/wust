angular.module("wust.components").controller("NavigationCtrl", NavigationCtrl);

NavigationCtrl.$inject = ["Auth", "SearchService"];

function NavigationCtrl(Auth, SearchService) {
    let vm = this;

    vm.navbarCollapsed = true;

    vm.newUser = {
        identifier: "",
        password: ""
    };

    let searchTriggerDelay = 300;

    vm.onSearchBoxChange = onSearchBoxChange;
    vm.authenticate = authenticate;
    vm.getUsername = Auth.getUsername.bind(Auth);
    vm.loggedIn = Auth.loggedIn.bind(Auth);
    vm.logout = Auth.logout.bind(Auth);
    vm.search = SearchService.search;
    vm.delayedTriggerSearch = undefined;

    function authenticate(register) {
        let func = register ? Auth.register : Auth.login;
        func.bind(Auth, angular.copy(vm.newUser))();
        vm.newUser.identifier = "";
        vm.newUser.password = "";
    }

    function onSearchBoxChange() {
        if( SearchService.search.query === "" )
            SearchService.search.resultsVisible = false;
        else {
            console.log("change");
            if( vm.delayedTriggerSearch ) clearTimeout(vm.delayedTriggerSearch);
            vm.delayedTriggerSearch = setTimeout(() => {console.log("triggerSearch");SearchService.triggerSearch();}, searchTriggerDelay);
            SearchService.search.resultsVisible = true;
        }
    }

}
