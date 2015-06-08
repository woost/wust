angular.module("wust.services").service("SearchService", SearchService);

SearchService.$inject = ["Search", "DiscourseNode"];

function SearchService(Search, DiscourseNode) {
    this.search = {
        resultsVisible: false,
        query: "",
        results: Search.$collection(),
        searchDescriptions: false,
        triggerSearch
    };

    function triggerSearch() {
        this.results.$refresh({
            label: DiscourseNode.Post.label,
            title: this.query,
            searchDescriptions: this.searchDescriptions
        });
    }
}
