angular.module("wust.components").controller("SearchCtrl", SearchCtrl);

SearchCtrl.$inject = ["SearchService", "$rootScope"];

function SearchCtrl(SearchService, $rootScope) {
    let vm = this;

    vm.search = SearchService.search;

    $rootScope.$on("$stateChangeSuccess", () => {
        vm.search.resultsVisible = false;
    });
}
