angular.module("wust.services").service("SearchService", SearchService);

SearchService.$inject = ["Search", "DiscourseNode"];

function SearchService(Search, DiscourseNode) {
    let defaultSize = 30;
    this.search = {
        resultsVisible: false,
        query: "",
        results: Search.$collection(),
        searchDescriptions: false,
        tagOr: false,
        selectedTags: [],
        waiting: true,
        page: 0,
        size: defaultSize,
        triggerSearch,
        loadMore
    };

    _.bindAll(this.search);

    function triggerSearch() {
        if (!this.size)
            return;

        this.page = 0;
        this.waiting = true;
        this.results.$refresh(getParams.apply(this));
        return this.results.$then(searchFinished.bind(this), searchFinished.bind(this));

        function searchFinished(val) {
            this.waiting = false;
        }
    }

    function loadMore() {
        if (!this.size)
            return;

        this.page++;
        return this.results.$fetch(getParams.apply(this));
    }

    function getParams() {
        return {
            label: DiscourseNode.Post.label,
            title: this.query,
            searchDescriptions: this.searchDescriptions,
            tagOr: this.tagOr,
            tags: this.selectedTags.map(t => t.id),
            page: this.page,
            size: this.size
        };
    }
}
