angular.module("wust.services").service("HistoryService", HistoryService);

HistoryService.$inject = ["Post", "DiscourseNode", "store"];

function HistoryService(Post, DiscourseNode, store) {
    let historyStore = store.getNamespacedStore("history");
    let maximum = 8;
    let self = this;

    this.activeViewIndex = historyStore.get("activeViewIndex") || 0;
    this.visited = [];
    _.each(historyStore.get("visited"), restoreNode);
    this.add = addNode;
    this.remove = removeNode;
    this.changeActiveView = changeActiveView;

    function restoreNode(id) {
        Post.$find(id).$then(node => addNode(node));
    }

    function changeActiveView(index) {
        self.activeViewIndex = index;
        historyStore.set("activeViewIndex", self.activeViewIndex);
    }

    function storeVisited() {
        historyStore.set("visited", _.map(self.visited, n => n.id));
    }

    function removeNode(id) {
        _.remove(self.visited, n => id === n.id);
        storeVisited();
    }

    function addNode(node) {
        _.remove(self.visited, n => node.id === n.id);
        self.visited.push(node);
        self.visited.splice(0, self.visited.length - maximum);
        storeVisited();
    }
}
