angular.module("wust.services").service("SearchService", SearchService);

SearchService.$inject = ["Search", "DiscourseNode"];

function SearchService(Search, DiscourseNode) {
    this.search = {
        resultsVisible: false,
        query: "",
        results: Search.$collection()
    };

    this.triggerSearch = triggerSearch;

    function triggerSearch() {
        this.search.results.$refresh({
            label: DiscourseNode.Post.label,
            title: this.search.query
        });
    }
}
