angular.module("wust.elements").directive("discourseNodeList", discourseNodeList);

discourseNodeList.$inject = [];

function discourseNodeList() {
    return {
        restrict: "A",
        templateUrl: "elements/node_list/discourse_node_list.html",
        scope: {
            nodeModel: "=",
            isLoading: "="
        },
        controller: discourseNodeListCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

discourseNodeListCtrl.$inject = [];

function discourseNodeListCtrl() {
    let vm = this;

    vm.symbolAction = {
        handler: remove,
        title: "disconnect",
        class: "fa fa-eye"
    };

    function remove(node) {
        vm.nodeModel.remove(node);
    }
}