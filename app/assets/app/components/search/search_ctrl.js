angular.module("wust.components").controller("SearchCtrl", SearchCtrl);

SearchCtrl.$inject = ["SearchService", "$rootScope"];

function SearchCtrl(SearchService, $rootScope) {
    let vm = this;

    vm.search = SearchService.search;
    vm.closeResults = closeResults;
    function closeResults() {
        SearchService.search.resultsVisible = false;
        SearchService.search.query = "";
    }

    $rootScope.$on("$stateChangeSuccess", () => {
        vm.search.resultsVisible = false;
    });
}
