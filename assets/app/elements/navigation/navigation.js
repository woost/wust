angular.module("wust.elements").directive("navigation", navigation);

navigation.$inject = [];

function navigation() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "elements/navigation/navigation.html",
        scope: true,
        controller: navigationCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

navigationCtrl.$inject = ["$state", "Auth", "SearchService", "ModalEditService", "FullscreenService", "KarmaService", "ContextService"];

function navigationCtrl($state, Auth, SearchService, ModalEditService, FullscreenService, KarmaService, ContextService) {
    let vm = this;

    vm.navbarCollapsed = true;

    vm.newUser = {
        identifier: "",
        password: ""
    };

    let searchTriggerDelay = 200;
    let delayedTriggerSearch;

    vm.onSearchBoxChange = onSearchBoxChange;
    vm.authenticate = authenticate;
    vm.Auth = Auth;
    vm.karma = KarmaService.karma;
    vm.logout = Auth.logout;
    vm.search = SearchService.search;
    vm.newDiscussion = newDiscussion;
    vm.fullscreen = FullscreenService;
    vm.$state = $state;
    vm.currentContexts = ContextService.currentContexts;
    vm.contextStyle = ContextService.contextStyle;
    vm.onContextChange = () => ContextService.emitChangedEvent();

    function authenticate(register) {
        let func = register ? Auth.register : Auth.login;
        func(angular.copy(vm.newUser));
        vm.newUser.identifier = "";
        vm.newUser.password = "";
    }

    function newDiscussion() {
        ModalEditService.show();
    }

    let lastSearch;
    function onSearchBoxChange() {
        if (lastSearch === SearchService.search.query)
            return;

        lastSearch = SearchService.search.query;
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
