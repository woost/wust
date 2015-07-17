angular.module("wust.services").service("StreamService", StreamService);

StreamService.$inject = ["Tag", "store"];

function StreamService(Tag, store) {
    let streamStore = store.getNamespacedStore("stream");
    let self = this;

    this.streams = [];
    restoreList();
    this.push = pustList;
    this.persist = storeList;
    this.forget = clearList;

    function restoreList() {
        _.each(streamStore.get("streams") || [], pustList);
    }

    function pustList(tags) {
        if (!_.isArray(tags) || _.isEmpty(tags))
            return;

        console.log(tags);
        let stream = {
            //TODO: search posts with all tags anstead of only first one
            posts: Tag.$buildRaw(tags[0]).posts.$search(),
            tags: _.map(tags, t => t.$encode ? t.$encode() : t)
        };

        self.streams.push(stream);
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
