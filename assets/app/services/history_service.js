angular.module("wust.services").service("HistoryService", HistoryService);

HistoryService.$inject = ["Auth", "Session", "Post", "DiscourseNode", "$rootScope", "$state"];

function HistoryService(Auth, Session, Post, DiscourseNode, $rootScope, $state) {
    let maximum = 8;
    let self = this;

    this.visited = [];
    this.add = addNode;
    this.remove = removeNode;
    this.load = load;
    this.forget = forget;

    this.updateCurrentView = updateCurrentView;
    this.addConnectToCurrentView = addConnectToCurrentView;
    this.removeFromCurrentView = removeFromCurrentView;
    this.addNodesToCurrentView = addNodesToCurrentView;

    let currentCommitUnregister, currentViewComponent;
    this.setCurrentViewComponent = setCurrentViewComponent;

    if (Auth.isLoggedIn) {
        load();
    }

    function setCurrentViewComponent(component) {
        if (currentCommitUnregister)
            currentCommitUnregister();

        currentViewComponent = component;
        currentCommitUnregister = currentViewComponent.onCommit(() => {
            $rootScope.$broadcast("component.changed");
        });
    }

    function load() {
        Session.history().then(response => {
            self.visited = _.values(response.$encode());
        });
    }

    function forget() {
        self.visited = [];
    }

    function updateCurrentView(node) {
        if (currentViewComponent === undefined)
            return;

        node = node.encode ? node.encode() : node;
        let current = currentViewComponent.getWrap("graph");
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

    function addNodesToCurrentView(nodes = [], relations = []) {
        if (currentViewComponent === undefined)
            return;

        let current = currentViewComponent.getWrap("graph");
        nodes.forEach(n => current.addNode(n));
        relations.forEach(n => current.addRelation(n));
        current.commit();
    }

    function addConnectToCurrentView(response, refId) {
        if (currentViewComponent === undefined)
            return;

        let current = currentViewComponent.getWrap("graph");
        if (refId !== undefined && !_.any(current.nodes, {id: refId}))
            return;

        response.graph.nodes.forEach(n => current.addNode(n));
        response.graph.relations.forEach(r => current.addRelation(r));
        current.commit();
    }

    function removeFromCurrentView(nodeId) {
        if (currentViewComponent === undefined)
            return;

        let current = currentViewComponent.getWrap("graph");
        if (current.rootNode.id === nodeId) {
            // TODO: not possible to have a component without a rootnode, so just notify the user, that the node was deleted
            if ($state.is("focus"))
                humane.success("Currently viewed Post has been deleted");
        } else {
            current.removeNode(nodeId);
            current.commit();
        }

        _.remove(self.visited, { id: nodeId });
    }

    function removeNode(id) {
        _.remove(self.visited, n => id === n.id);
    }

    function addNode(node) {
        node = node.encode ? node.encode() : node;
        _.remove(self.visited, n => node.id === n.id);
        self.visited.splice(0, 0, node);
        self.visited.splice(maximum, self.visited.length - maximum);
    }
}
