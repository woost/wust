app.directive("changeItemList", function($compile, $stateParams) {
    return {
        restrict: 'A',
        templateUrl: 'assets/views/change_item_list.html',
        scope: {
            items: '=',
        },
        link: function(scope, element, attrs) {
        }
    };
});
