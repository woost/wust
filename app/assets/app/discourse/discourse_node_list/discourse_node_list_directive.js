angular.module("wust.discourse").directive("discourseNodeList", discourseNodeList);

discourseNodeList.$inject = [];

function discourseNodeList() {
    return {
        restrict: "A",
        templateUrl: "assets/app/discourse/discourse_node_list/discourse_node_list.html",
        scope: {
            nodeModel: "=",
        },
        controller: discourseNodeListCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

discourseNodeListCtrl.$inject = [];

function discourseNodeListCtrl() {}
