angular.module("wust.services").service("HistoryService", HistoryService);

HistoryService.$inject = ["Session", "Post", "DiscourseNode"];

function HistoryService(Session, Post, DiscourseNode) {
    let maximum = 8;
    let self = this;

    this.visited = [];
    this.add = addNode;
    this.remove = removeNode;
    this.load = load;
    this.forget = forget;

    this.updateCurrentView = updateCurrentView;
    this.addConnectToCurrentView = addConnectToCurrentView;

    this.currentViewComponent = undefined;

    function load() {
        Session.history.$fetch().$then(response => {
            self.visited = _.values(response.$encode());
        });
    }

    function forget() {
        self.visited = [];
    }

    function updateCurrentView(node) {
        if (this.currentViewComponent === undefined)
            return;

        //TODO: check whether node is tags relation and then update accordingly

        let current = this.currentViewComponent.getWrap("graph");
        let existing = _.find(current.nodes, _.pick(node, "id"));
        if (existing !== undefined) {
            _.assign(existing, node);
            current.commit();
        }

        let existingVisit = _.find(self.visited, _.pick(node, "id"));
        if (existingVisit !== undefined) {
            _.assign(existingVisit, node);
        }
    }

    function addConnectToCurrentView(refId, response) {
        if (this.currentViewComponent === undefined)
            return;

        let current = this.currentViewComponent.getWrap("graph");
        if (!_.any(current.nodes, {id: refId}))
            return;

        response.graph.nodes.forEach(n => current.addNode(n));
        response.graph.relations.forEach(r => current.addRelation(r));
        current.commit();
    }

    function removeNode(id) {
        _.remove(self.visited, n => id === n.id);
    }

    function addNode(node) {
        _.remove(self.visited, n => node.id === n.id);
        self.visited.splice(0, self.visited.length - maximum - 1);
        self.visited.splice(0, 0, node);
    }
}
