app.directive("showDiscourseNodeList", function($compile, $stateParams) {
    return {
        restrict: 'A',
        templateUrl: 'assets/views/show_discourse_node_list.html',
        scope: {
            items: '=',
        },
        link: function(scope, element, attrs) {
        }
    };
});
