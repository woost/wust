angular.module("wust.components").controller("SearchCtrl", SearchCtrl);

SearchCtrl.$inject = ["SearchService", "DiscourseNode", "$rootScope"];

function SearchCtrl(SearchService, DiscourseNode, $rootScope) {
    let vm = this;

    vm.search = SearchService.search;
    vm.nodeInfo = DiscourseNode.Post;

    $rootScope.$on("$stateChangeSuccess", () => {
        vm.search.resultsVisible = false;
    });
}
