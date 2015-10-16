angular.module("wust.elements").directive("discourseNodeList", discourseNodeList);

discourseNodeList.$inject = [];

function discourseNodeList() {
    return {
        restrict: "A",
        templateUrl: "elements/node_list/discourse_node_list.html",
        scope: {
            nodeModel: "=",
            isLoading: "=",
            templateBefore: "@",
            templateAfter: "@",
        },
        controller: discourseNodeListCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

discourseNodeListCtrl.$inject = ["Post", "TagRelationEditService", "Auth"];

function discourseNodeListCtrl(Post, TagRelationEditService, Auth) {
    let vm = this;

    vm.Auth = Auth;
    vm.upvoteAnswer = upvoteAnswer;
    vm.remove = remove;
    vm.editFollowerConnects = editFollowerConnects;
    vm.editPredecessorConnects = editPredecessorConnects;

    vm.connectsRelations = {};
    let deregisterCommit = vm.nodeModel.component.onCommit(() => {
        vm.connectsRelations = vm.nodeModel.component.rootNode.outRelations.map(rel => {
            return {
                [rel.endNode.id]: rel
            };
        }).reduce(_.merge, {});
        deregisterCommit();
    });

    function upvoteAnswer(connectable) {
        let connects = findConnects(vm.nodeModel.component.rootNode, connectable);
        vm.connectsRelations[connectable.id] = connects;
        let service = Post.$buildRaw(_.pick(vm.nodeModel.component.rootNode, "id")).connectsTo.$buildRaw(_.pick(connectable, "id"));
        if (connects.vote) {
            service.neutral.$create().$then(data => {
                connects.vote = undefined;
                connects.quality = data.quality;
                // humane.success("Unvoted post as answer");
            }, resp => humane.error(resp.$response.data));
        } else {
            service.up.$create().$then(data => {
                connects.vote = data.vote;
                connects.quality = data.quality;
                // humane.success("Upvoted post as answer");
            }, resp => humane.error(resp.$response.data));
        }
    }

    function remove(node) {
        vm.nodeModel.remove(node);
    }

    function findConnects(startNode, endNode) {
        return _.find(startNode.outRelations, h => h.endNode.id === endNode.id);
    }

    function editPredecessorConnects(node) {
        let connects = findConnects(vm.nodeModel.node, node);
        TagRelationEditService.show(connects, () => vm.nodeModel.remove(node));
        //TODO: update startNode, endNode and bigpost taglist
    }

    function editFollowerConnects(node) {
        let connects = findConnects(node, vm.nodeModel.node);
        TagRelationEditService.show(connects, () => vm.nodeModel.remove(node));
        //TODO: update startNode, endNode and bigpost taglist
    }
}
