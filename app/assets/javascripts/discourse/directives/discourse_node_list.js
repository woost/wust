angular.module("wust.discourse").directive("discourseNodeList", function() {
    return {
        restrict: "A",
        require: "ngModel",
        templateUrl: "assets/partials/discourse/discourse_node_list.html",
        scope: {
            nodeModel: "=",
        }
    };
});
