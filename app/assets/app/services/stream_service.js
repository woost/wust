angular.module("wust.services").service("StreamService", StreamService);

StreamService.$inject = ["Search", "DiscourseNode", "store"];

function StreamService(Search, DiscourseNode, store) {
    let streamStore = store.getNamespacedStore("stream");
    let self = this;

    this.streams = [];
    restoreList();
    this.push = pushList;
    this.persist = storeList;
    this.remove = removeList;
    this.forget = clearList;

    function restoreList() {
        _.each(streamStore.get("streams") || [], pushList);
    }

    function pushList(tags) {
        if (!_.isArray(tags) || _.isEmpty(tags))
            return;

        let stream = {
            //TODO: search posts with all tags anstead of only first one
            posts: Search.$search({
                label: DiscourseNode.Post.label,
                tags: tags.map(t => t.id)
            }),
            tags: _.map(tags, t => t.encode ? t.encode() : t)
        };

        self.streams.push(stream);
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
        streamStore.set("streams", _.map(self.streams, t => t.tags));
    }
}
