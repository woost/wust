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

discourseNodeListCtrl.$inject = ["Connectable"];

function discourseNodeListCtrl(Connectable) {
    let vm = this;

    vm.upvoteAnswer = upvoteAnswer;

    vm.symbolAction = vm.nodeModel.writable ? {
        handler: remove,
        title: "disconnect",
        class: "fa fa-scissors"
    } : undefined;

    //TODO: unvote
    function upvoteAnswer(connectable) {
        Connectable.$buildRaw(_.pick(vm.nodeModel.component.rootNode, "id")).connectsTo.$buildRaw(_.pick(connectable, "id")).up.$create().$then(() => {
            humane.success("Upvoted post as answer");
        }, resp => {
            humane.error(resp.$response.data);
        });
    }

    function remove(node) {
        vm.nodeModel.remove(node);
    }
}
