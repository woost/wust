angular.module("wust.components").controller("SearchCtrl", SearchCtrl);

SearchCtrl.$inject = ["SearchService", "DiscourseNode"];

function SearchCtrl(SearchService, DiscourseNode) {
    let vm = this;

    vm.search = SearchService.search;
    vm.nodeInfo = DiscourseNode.Post;
}
