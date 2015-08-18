angular.module("wust.elements").directive("navigation", navigation);

navigation.$inject = [];

function navigation() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "assets/app/elements/navigation/navigation.html",
        scope: true,
        controller: navigationCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

navigationCtrl.$inject = ["$state", "Auth", "SearchService", "ModalEditService"];

function navigationCtrl($state, Auth, SearchService, ModalEditService) {
    let vm = this;

    vm.navbarCollapsed = true;

    vm.newUser = {
        identifier: "",
        password: ""
    };

    let searchTriggerDelay = 300;
    let delayedTriggerSearch;

    vm.onSearchBoxChange = onSearchBoxChange;
    vm.authenticate = authenticate;
    vm.currentAuth = Auth.current;
    vm.logout = Auth.logout;
    vm.search = SearchService.search;
    vm.modalEdit = ModalEditService;
    vm.$state = $state;

    function authenticate(register) {
        let func = register ? Auth.register : Auth.login;
        func(angular.copy(vm.newUser));
        vm.newUser.identifier = "";
        vm.newUser.password = "";
    }

    function onSearchBoxChange() {
        if(!SearchService.search.query) {
            SearchService.search.resultsVisible = false;
        } else {
            if(delayedTriggerSearch)
                clearTimeout(delayedTriggerSearch);

            delayedTriggerSearch = setTimeout(() => SearchService.search.triggerSearch(), searchTriggerDelay);
            SearchService.search.resultsVisible = true;
        }
    }
}
