angular.module("wust.services").service("SearchService", SearchService);

SearchService.$inject = ["Search", "DiscourseNode"];

function SearchService(Search, DiscourseNode) {
    this.search = {
        resultsVisible: false,
        query: "",
        results: Search.$collection(),
        searchDescriptions: false,
        tagOr: false,
        selectedTags: [],
        waiting: true,
        page: 0,
        size: 30,
        noMore: false,
        triggerSearch,
        loadMore
    };

    function triggerSearch() {
        this.noMore = false;
        this.page = 0;
        this.waiting = true;
        this.results.$refresh(getParams.apply(this));
        this.results.$then(searchFinished.bind(this), searchFinished.bind(this));

        function searchFinished(val) {
            this.waiting = false;
            this.noMore = val.length < this.size;
        }
    }

    function loadMore() {
        if (this.noMore)
            return;

        this.page++;
        let prevLength = this.results.length;
        this.results.$fetch(getParams.apply(this)).$then(val => {
            let diff = val.length - prevLength;
            this.noMore = diff < this.size;
        });
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
