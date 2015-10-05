angular.module("wust.services").service("StreamService", StreamService);

StreamService.$inject = ["Search", "ContextService", "DiscourseNode", "store", "Helpers", "$state"];

function StreamService(Search, ContextService, DiscourseNode, store, Helpers, $state) {
    let streamStore = store.getNamespacedStore("stream");
    let self = this;

    this.streams = [];
    restoreList();
    this.push = pushList;
    this.persist = storeList;
    this.remove = removeList;
    this.forget = clearList;
    this.refreshStream = refreshStream;
    this.refreshDashboard = refreshDashboard;
    this.currentEditStream = undefined;

    this.recentPosts = Search.$search({
        label: DiscourseNode.Post.label,
        tagsAll: ContextService.currentContexts.map(c => c.id),
        size: 30,
        page: 0,
        startPost: true
    });

    function restoreList() {
        _.each(streamStore.get("streams") || [], pushList);
    }

    function pushList(streamDef = {}) {
        streamDef.tagsAll = streamDef.tagsAll || [];
        streamDef.tagsAny = streamDef.tagsAny || [];
        streamDef.tagsWithout = streamDef.tagsWithout || [];

        let stream = {
            posts: Search.$search(searchParams(streamDef)),
            tagsAll: streamDef.tagsAll,
            tagsAny: streamDef.tagsAny,
            tagsWithout: streamDef.tagsWithout,
        };

        self.streams.push(stream);
        storeList();
    }

    function searchParams(streamDef) {
        return {
            label: DiscourseNode.Post.label,
            tagsAll: streamDef.tagsAll.filter(t => t.isContext).map(t => t.id),
            tagsAny: streamDef.tagsAny.filter(t => t.isContext).map(t => t.id),
            tagsWithout: streamDef.tagsWithout.filter(t => t.isContext).map(t => t.id),
            classificationsAll: streamDef.tagsAll.filter(t => !t.isContext).map(t => t.id),
            classificationsAny: streamDef.tagsAny.filter(t => !t.isContext).map(t => t.id),
            classificationsWithout: streamDef.tagsWithout.filter(t => !t.isContext).map(t => t.id),
            size: 20,
            page: 0,
        };
    }

    function refreshDashboard(event) {
        if ($state.is("dashboard")) {
            self.recentPosts.$refresh();
            self.streams.forEach(s => s.posts.$refresh());
        }
    }

    function refreshStream(stream) {
        stream.posts.$refresh(searchParams(stream));

        storeList();
    }

    function removeList(index) {
        self.streams.splice(index, 1);
        storeList();
    }

    function clearList() {
        self.streams = [];
        storeList();
    }

    function storeList() {
        streamStore.set("streams", _.map(self.streams, s => { return {
            tagsAll: s.tagsAll.map(t => t.encode ? t.encode() : t),
            tagsAny: s.tagsAny.map(t => t.encode ? t.encode() : t),
            tagsWithout: s.tagsWithout.map(t => t.encode ? t.encode() : t),
        };}));
    }
}
