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
        _.each(streamStore.get("streams") || [], pushList);
    }

    function pushList(tags) {
        if (!_.isArray(tags) || _.isEmpty(tags))
            return;

        let stream = {
            posts: Search.$search({
                label: DiscourseNode.Post.label,
                tags: tags.map(t => t.id),
                size: 20,
                page: 0
            }),
            tags: tags
        };

        self.streams.push(stream);
        storeList();
    }

    function refreshStream(stream, tags) {
        if (_.isEmpty(tags)) {
            _.remove(self.streams, stream);
        } else {
            stream.tags = tags;
            stream.posts.$refresh({
                tags: tags.map(t => t.id)
            });
        }

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
        streamStore.set("streams", _.map(self.streams, s => s.tags.map(t => t.encode ? t.encode() : t)));
    }
}
