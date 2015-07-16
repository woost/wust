angular.module("wust.services").service("StreamService", StreamService);

StreamService.$inject = ["Tag", "store"];

function StreamService(Tag, store) {
    let streamStore = store.getNamespacedStore("stream");
    let self = this;

    this.streams = [];
    restoreStack();
    this.push = pushStack;
    this.persist = storeStack;
    this.forget = clearStack;

    function restoreStack() {
        _.each(streamStore.get("streams") || [], pushStack);
    }

    function pushStack(tags) {
        if (!_.isArray(tags) || _.isEmpty(tags))
            return;

        console.log(tags);
        let stream = {
            //TODO: search posts with all tags anstead of only first one
            posts: Tag.$buildRaw(tags[0]).posts.$search(),
            tags: _.map(tags, t => t.$encode ? t.$encode() : t)
        };

        self.streams.push(stream);
        storeStack();
    }

    function clearStack() {
        self.streams = [];
        storeStack();
    }

    function storeStack() {
        streamStore.set("streams", _.map(self.streams, t => t.tags));
    }
}
