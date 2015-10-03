angular.module("wust.services").service("StreamService", StreamService);

StreamService.$inject = ["Search", "DiscourseNode", "store", "Helpers"];

function StreamService(Search, DiscourseNode, store, Helpers) {
    let streamStore = store.getNamespacedStore("stream");
    let self = this;

    this.streams = [];
    restoreList();
    this.push = pushList;
    this.persist = storeList;
    this.remove = removeList;
    this.forget = clearList;
    this.refreshStream = refreshStream;
    this.currentEditStream = undefined;

    function restoreList() {
        _.each(streamStore.get("streams") || [], stream => pushList(stream.tagsAll, stream.tagsWithout));
    }

    function pushList(tagsAll = [], tagsWithout = []) {
        if (!_.isArray(tagsAll) || !_.isArray(tagsWithout))
            return;

        let stream = {
            posts: Search.$search({
                label: DiscourseNode.Post.label,
                tagsAll: tagsAll.map(t => t.id),
                tagsWithout: tagsWithout.map(t => t.id),
                size: 20,
                page: 0
            }),
            tagsAll: tagsAll,
            tagsWithout: tagsWithout
        };

        self.streams.push(stream);
        storeList();
    }

    function refreshStream(stream) {
        stream.posts.$refresh({
            tagsAll: stream.tagsAll.map(t => t.id),
            tagsWithout: stream.tagsWithout.map(t => t.id)
        });

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
            tagsWithout: s.tagsWithout.map(t => t.encode ? t.encode() : t)
        };}));
    }
}
