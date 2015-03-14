angular.module("wust").directive("showDiscourseNodeList", function($compile, $stateParams) {
    return {
        restrict: 'A',
        require: 'ngModel',
        templateUrl: 'assets/partials/discourse_node_list.html',
        scope: {
            ngModel: '=',
        }
    };
});
