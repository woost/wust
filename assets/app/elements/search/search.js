angular.module("wust.elements").directive("search", search);

search.$inject = [];

function search() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "elements/search/search.html",
        scope: true,
        controller: searchCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

searchCtrl.$inject = ["$scope", "SearchService"];

function searchCtrl($scope, SearchService) {
    let vm = this;

    vm.search = SearchService.search;
    vm.search.onReload(() => {
        if (vm.infinite)
            vm.infinite.initialize();
    });
}
