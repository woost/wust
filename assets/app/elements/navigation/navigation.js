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

navigationCtrl.$inject = ["$state", "Auth", "SearchService", "ModalEditService", "FullscreenService", "KarmaService", "ContextService", "Session"];

function navigationCtrl($state, Auth, SearchService, ModalEditService, FullscreenService, KarmaService, ContextService, Session) {
    let vm = this;

    vm.navbarCollapsed = true;

    vm.newUser = {
        identifier: "",
        password: ""
    };

    vm.session = Session;
    vm.onSearchBoxChange = onSearchBoxChange;
    vm.authenticate = authenticate;
    vm.Auth = Auth;
    vm.karma = KarmaService.karma;
    vm.logout = Auth.logout;
    vm.search = SearchService.search;
    vm.newDiscussion = newDiscussion;
    vm.fullscreen = FullscreenService;
    vm.$state = $state;

    let brandingColor = window.globals.uiSettings.brandingColor;
    if (brandingColor) {
        vm.contextStyle = {
            backgroundColor: brandingColor,
            borderBottom: "1px solid " + d3.hcl(d3.rgb(brandingColor)).darker().toString()
        };
    }

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
    let searchTriggerDelay = 200;
    let delayedTriggerSearch;
    function onSearchBoxChange(isSubmit = false) {
        if (isSubmit)
            SearchService.search.resultsVisible = true;

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
