angular.module("wust").directive("discourseNodeList", function() {
    return {
        restrict: "A",
        require: "ngModel",
        templateUrl: "assets/partials/discourse_node_list.html",
        scope: {
            nodeModel: "=",
        }
    };
});
