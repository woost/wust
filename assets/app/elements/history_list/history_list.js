angular.module("wust.elements").directive("historyList", historyList);

historyList.$inject = [];

function historyList() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "elements/history_list/history_list.html",
        scope: true,
        controller: HistoryListCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

HistoryListCtrl.$inject = ["HistoryService", "SearchService"];

function HistoryListCtrl(HistoryService, SearchService) {
    let vm = this;

    vm.history = HistoryService;
    vm.search = SearchService.search;
    vm.onSearchBoxChange = onSearchBoxChange;

    let lastSearch;
    let searchTriggerDelay = 200;
    let delayedTriggerSearch;
    function onSearchBoxChange(force = false) {
        if (!force && lastSearch === SearchService.search.query)
            return;

        lastSearch = SearchService.search.query;
        if(SearchService.search.query) {
            if(delayedTriggerSearch)
                clearTimeout(delayedTriggerSearch);

            delayedTriggerSearch = setTimeout(() => SearchService.search.triggerSearch(), searchTriggerDelay);
        }
    }
}
