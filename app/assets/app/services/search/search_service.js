angular.module("wust.services").service("SearchService", SearchService);

SearchService.$inject = ["Search", "DiscourseNode"];

function SearchService(Search, DiscourseNode) {
    this.search = {
        resultsVisible: false,
        query: "",
        results: Search.$collection(),
        searchDescriptions: false,
        waiting: true,
        triggerSearch
    };

    function triggerSearch() {
        this.waiting = true;
        this.results.$refresh({
            label: DiscourseNode.Post.label,
            title: this.query,
            searchDescriptions: this.searchDescriptions
        });
        let self = this;
        this.results.$then(() => self.waiting = false, () => self.waiting = false);
    }
}
