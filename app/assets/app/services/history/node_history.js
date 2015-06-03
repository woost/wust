angular.module("wust.services").service("NodeHistory", NodeHistory);

NodeHistory.$inject = ["Post", "DiscourseNode", "store"];

function NodeHistory(Post, DiscourseNode, store) {
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
        historyStore.set("visited", _.map(self.visited, n => n.node.id));
    }

    function remove(id) {
        _.remove(self.visited, item => id === item.node.id);
        storeVisited();
    }

    function addNode(node, info) {
        _.remove(self.visited, n => node.id === n.node.id);
        self.visited.push({
            node,
            info: DiscourseNode.get(node.label)
        });

        self.visited.splice(0, self.visited.length - maximum);
        storeVisited();
    }

    function add(promise) {
        promise.$then(addNode);
    }
}
