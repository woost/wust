angular.module("wust.elements").directive("staticEditPost", staticEditPost);

staticEditPost.$inject = [];

function staticEditPost() {
    return {
        restrict: "A",
        templateUrl: "elements/post/static_edit_post.html",
        scope: {
            node: "=",
            alwaysShowTagSuggestions: "@",
            onSave: "&",
            onCancel: "&",
            onDelete: "&"
        },
        controller: StaticEditPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

StaticEditPostCtrl.$inject = ["$state", "$scope", "Auth", "EditService", "KarmaService", "HistoryService"];

function StaticEditPostCtrl($state, $scope, Auth, EditService, KarmaService, HistoryService) {
    let vm = this;

    vm.deleteNode = deleteNode;
    vm.saveNode = saveNode;
    vm.editNode = EditService.edit(vm.node);

    let authorBoost = Auth.current.userId === vm.node.author.id ? wust.Moderation().authorKarmaBoost : 0;
    $scope.$on("component.changed", calculateEditWeight);
    $scope.$on("karma.changed", calculateEditWeight);
    calculateEditWeight();

    function saveNode() {
        let promise = vm.editNode.save();
        if (promise)
            promise.$then(data => vm.onSave({response: data}));
    }

    function deleteNode() {
        vm.editNode.deleteNode().$then(data => vm.onDelete({response: data}));
    }

    function calculateEditWeight() {
        //TODO: FIXME: this is used in the neighbours graph, but successors of hyperrelations is broken in hyper graph wrappers, so get it the node from the current component:
        let comp = HistoryService.getCurrentViewComponentGraph();
        let node = _.find(comp.nodes, {id: vm.node.id});
        if (node)
            vm.editWeight = KarmaService.voteWeightInContexts(node.connectedTags) + authorBoost;
        else
            vm.editWeight = 0;

        vm.canApply = wust.Moderation().canApply(vm.editWeight, vm.node.postChangeThreshold);
    }
}
