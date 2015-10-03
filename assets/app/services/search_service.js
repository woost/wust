angular.module("wust.services").service("SearchService", SearchService);

SearchService.$inject = ["Search", "DiscourseNode"];

function SearchService(Search, DiscourseNode) {
    let defaultSize = 30;
    let reloadHandler;

    this.search = {
        query: "",
        searchDescriptions: false,
        searchStartPost: false,
        tagsAll: [],
        tagsAny: [],
        tagsWithout: [],
        page: 0,
        size: defaultSize,

        resultsVisible: false,
        results: Search.$collection(),
        waiting: true,
        unlimited: false,
        triggerSearch,
        loadMore,
        onReload
    };

    _.bindAll(this.search);

    function triggerSearch() {
        if (!this.size)
            return false;

        this.page = 0;
        this.waiting = true;
        this.results.$refresh(getParams.apply(this));
        this.results.$then(searchFinished.bind(this), searchFinished.bind(this));
        callReloadHandler();
        return true;

        function searchFinished(val) {
            this.waiting = false;
        }
    }

    function callReloadHandler() {
        if (reloadHandler)
            reloadHandler();
    }

    function onReload(handler) {
        if (handler)
            reloadHandler = handler;
    }

    function loadMore() {
        if (!this.size)
            return;

        this.page++;
        return this.results.$fetch(getParams.apply(this));
    }

    function getParams() {
        let params = {
            label: DiscourseNode.Post.label,
            term: this.query,
            searchDescriptions: this.searchDescriptions,
            startPost: this.searchStartPost,
            tagsAll: this.tagsAll.map(t => t.id),
            tagsAny: this.tagsAny.map(t => t.id),
            tagsWithout: this.tagsWithout.map(t => t.id),
        };

        if (this.unlimited) {
            return params;
        } else {
            return _.merge(params, {
                page: this.page,
                size: this.size
            });
        }
    }
}
