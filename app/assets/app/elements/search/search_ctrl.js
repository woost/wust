angular.module("wust.components").directive("search", search);

search.$inject = [];

function search() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/search/search.html",
        scope: true,
        controller: searchCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

searchCtrl.$inject = ["SearchService", "$rootScope"];

function searchCtrl(SearchService, $rootScope) {
    let vm = this;

    vm.search = SearchService.search;

    $rootScope.$on("$stateChangeStart", () => {
        vm.search.resultsVisible = false;
    });
}
