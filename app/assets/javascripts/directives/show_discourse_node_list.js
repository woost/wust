app.directive("showDiscourseNodeList", function($compile, $stateParams) {
    return {
        restrict: 'A',
        require: 'ngModel',
        templateUrl: 'assets/partials/discourse_node_list.html',
        scope: {
            ngModel: '=',
        },
        link: function(scope, element, attrs) {
        }
    };
});
