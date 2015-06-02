angular.module("wust.services").service("SearchService", SearchService);

SearchService.$inject = ["Search"];

function SearchService(Search) {
    this.search = {
        resultsVisible: false,
        query: "",
        results: Search.$collection()
    };

    this.triggerSearch = triggerSearch;

    function triggerSearch() {
        this.search.results.$refresh({title: this.search.query});
    }
}
