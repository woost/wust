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

navigationCtrl.$inject = ["$state", "Auth", "SearchService", "DiscourseNode", "Search", "ModalEditService", "FullscreenService", "Helpers", "ContextService"];

function navigationCtrl($state, Auth, SearchService, DiscourseNode, Search, ModalEditService, FullscreenService, Helpers, ContextService) {
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
    vm.currentAuth = Auth.current;
    vm.logout = Auth.logout;
    vm.search = SearchService.search;
    vm.newDiscussion = newDiscussion;
    vm.fullscreen = FullscreenService;
    vm.$state = $state;
    vm.currentContexts = ContextService.currentContexts;
    vm.onContextChange = onContextChange;

    function onContextChange() {
        vm.contextStyle = {
            "background-color": vm.currentContexts.length > 0 ? Helpers.hashToColorNavBg(vm.currentContexts[0]) : undefined
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
        //TODO: get tag by name
        Search.$search({
            title: "Startpost",
            label: DiscourseNode.TagLike.label,
            size: 1,
            page: 0
        }).$then(val => ModalEditService.currentNode.tags = val.$encode());
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
