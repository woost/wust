angular.module("wust.services").service("HistoryService", HistoryService);

HistoryService.$inject = ["Post", "DiscourseNode", "store"];

function HistoryService(Post, DiscourseNode, store) {
    let historyStore = store.getNamespacedStore("history");
    let maximum = 8;
    let self = this;

    this.visited = [];
    _.each(historyStore.get("visited"), restoreNode);
    this.add = add;
    this.remove = remove;

    function restoreNode(id) {
        Post.$find(id).$then(node => addNode(node));
    }

    function storeVisited() {
        historyStore.set("visited", _.map(self.visited, n => n.id));
    }

    function remove(id) {
        _.remove(self.visited, n => id === n.id);
        storeVisited();
    }

    function addNode(node) {
        _.remove(self.visited, n => node.id === n.id);
        self.visited.push(node);
        self.visited.splice(0, self.visited.length - maximum);
        storeVisited();
    }

    function add(promise) {
        promise.$then(addNode);
    }
}
