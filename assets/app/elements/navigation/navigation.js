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

navigationCtrl.$inject = ["$state", "Auth", "SearchService", "DiscourseNode", "Search", "ModalEditService", "FullscreenService", "Helpers"];

function navigationCtrl($state, Auth, SearchService, DiscourseNode, Search, ModalEditService, FullscreenService, Helpers) {
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
    vm.currentContext = {title:"Meta", color:Math.floor(Math.random()*360)};
    vm.contextStyle = {
        "background-color": Helpers.hashToColorNavBg(vm.currentContext)
    };

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
