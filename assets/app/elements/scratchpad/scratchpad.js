angular.module("wust.elements").directive("scratchpad", scratchpad);

scratchpad.$inject = [];

function scratchpad() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "elements/scratchpad/scratchpad.html",
        scope: true,
        controller: scratchpadCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

scratchpadCtrl.$inject = ["$state", "$stateParams", "HistoryService", "Session", "EditService", "SidebarService", "ContextService", "$q", "ConnectedComponents"];

function scratchpadCtrl($state, $stateParams, HistoryService, Session, EditService, SidebarService, ContextService, $q, ConnectedComponents) {
    let vm = this;

    let saveOnEnter = true;

    Session.marks.search().then(posts => {
        posts.forEach(post => EditService.edit(post, true));
    });

    vm.sidebar = SidebarService;
    vm.editList = EditService.list;
    vm.edit = EditService.edit;
    vm.editNewPost = editNewPost;
    vm.loadGraph = loadGraph;
    vm.options = EditService.scratchpad;
    vm.scratchpadNodes = scratchpadNodes;

    vm.newPost = {
        title: ""
    };

    function scratchpadNodes() {
        return vm.editList.filter(s => s.visible);
    }

    function loadGraph() {
        let scratchNodes = scratchpadNodes();
        if (_.isEmpty(scratchNodes))
            return;

        if ($state.is("focus", { id: $stateParams.id, type: "graph" })) {
            loadIntoGraph(scratchNodes);
        } else {
            let firstNode = scratchNodes[0];
            let restNodes = scratchNodes.slice(1, scratchNodes.length);
            $state.go("focus", { id: firstNode.id, type: "graph" }).then(() => loadIntoGraph(restNodes));
        }

        function loadIntoGraph(nodes) {
            $q.all(scratchNodes.map(node => ConnectedComponents.$find(node.id, {depth: 1}).$asPromise())).then(results => {
                let nodes = _.flatten(results.map(r => r.nodes));
                let relations = _.flatten(results.map(r => r.relations));
                HistoryService.addNodesToCurrentView(nodes, relations);
            });
        }
    }

    function editNewPost() {
        let session = EditService.edit(vm.newPost, true, 0);
        session.tags = angular.copy(ContextService.currentContexts);

        vm.newPost.title = "";

        if (saveOnEnter) {
            session.save();
        }
    }
}
