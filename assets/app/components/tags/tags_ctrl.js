angular.module("wust.components").controller("TagsCtrl", TagsCtrl);

TagsCtrl.$inject = ["$stateParams", "Search", "DiscourseNode"];

function TagsCtrl($stateParams, Search, DiscourseNode) {
    let vm = this;

    let tagSize = 30;
    let tagPage = 0;
    let delayedTriggerSearch;
    let searchTriggerDelay = 300;

    vm.loadMoreTags = loadMoreTags;
    vm.refreshSearch = refreshSearch;

    vm.search = {
        query: "",
        results: Search.$search({
            label: DiscourseNode.Scope.label,
            page: tagPage,
            size: tagSize
        }),
    };


    function refreshSearch() {
        tagPage = 0;
        if(delayedTriggerSearch)
            clearTimeout(delayedTriggerSearch);

        delayedTriggerSearch = setTimeout(() => vm.search.results.$refresh({
            page: tagPage,
            title: vm.search.query
        }));
    }

    function loadMoreTags() {
        tagPage++;
        return vm.search.results.$fetch({
            page: tagPage
        });
    }
}
