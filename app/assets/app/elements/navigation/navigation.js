angular.module("wust.components").directive("navigation", navigation);

navigation.$inject = [];

function navigation() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/navigation/navigation.html",
        scope: true,
        controller: navigationCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

navigationCtrl.$inject = ["Auth", "SearchService", "LeftSideService", "ModalEditService"];

function navigationCtrl(Auth, SearchService, LeftSideService, ModalEditService) {
    let vm = this;

    vm.navbarCollapsed = true;

    vm.newUser = {
        identifier: "",
        password: ""
    };

    let searchTriggerDelay = 300;

    vm.onSearchBoxChange = onSearchBoxChange;
    vm.authenticate = authenticate;
    vm.currentAuth = Auth.current;
    vm.loggedIn = Auth.loggedIn.bind(Auth);
    vm.logout = Auth.logout.bind(Auth);
    vm.search = SearchService.search;
    vm.delayedTriggerSearch = undefined;
    vm.modalEdit = ModalEditService;
    vm.leftSide = LeftSideService;
    vm.searchToggleDisabled = searchToggleDisabled;

    function authenticate(register) {
        let func = register ? Auth.register : Auth.login;
        func.bind(Auth, angular.copy(vm.newUser))();
        vm.newUser.identifier = "";
        vm.newUser.password = "";
    }

    function searchToggleDisabled() {
        return SearchService.search.query === "";
    }

    function onSearchBoxChange() {
        if(searchToggleDisabled())
            SearchService.search.resultsVisible = false;
        else {
            if( vm.delayedTriggerSearch ) clearTimeout(vm.delayedTriggerSearch);
            vm.delayedTriggerSearch = setTimeout(() => SearchService.search.triggerSearch(), searchTriggerDelay);
            SearchService.search.resultsVisible = true;
        }
    }
}
