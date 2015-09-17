angular.module("wust.elements").directive("discourseNodeList", discourseNodeList);

discourseNodeList.$inject = [];

function discourseNodeList() {
    return {
        restrict: "A",
        templateUrl: "elements/node_list/discourse_node_list.html",
        scope: {
            nodeModel: "=",
            isLoading: "=",
            templateBefore: "@"
        },
        controller: discourseNodeListCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

discourseNodeListCtrl.$inject = ["Post", "TagRelationEditService"];

function discourseNodeListCtrl(Post, TagRelationEditService) {
    let vm = this;

    vm.upvoteAnswer = upvoteAnswer;
    vm.remove = remove;
    vm.editFollowerConnects = editFollowerConnects;
    vm.editPredecessorConnects = editPredecessorConnects;

    //TODO: unvote
    function upvoteAnswer(connectable) {
        Post.$buildRaw(_.pick(vm.nodeModel.component.rootNode, "id")).connectsTo.$buildRaw(_.pick(connectable, "id")).up.$create().$then(() => {
            humane.success("Upvoted post as answer");
        }, resp => {
            humane.error(resp.$response.data);
        });
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
