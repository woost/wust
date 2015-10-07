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

scratchpadCtrl.$inject = ["$state", "HistoryService", "Session", "EditService", "SidebarService", "ContextService"];

function scratchpadCtrl($state, HistoryService, Session, EditService, SidebarService, ContextService) {
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

        let firstNode = scratchNodes[0];
        let restNodes = scratchNodes.slice(1, scratchNodes.length);
        $state.go("focus", { id: firstNode.id, type: "graph" }).then(() => {
            _.defer(() => HistoryService.addNodesToCurrentView(restNodes));
        });
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
