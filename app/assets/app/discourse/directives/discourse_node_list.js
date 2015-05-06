angular.module("wust.discourse").directive("discourseNodeList", discourseNodeList);

discourseNodeList.$inject = [];

function discourseNodeList() {
    return {
        restrict: "A",
        require: "ngModel",
        templateUrl: "assets/app/discourse/directives/discourse_node_list.html",
        scope: {
            nodeModel: "=",
        }
    };
}
