app.directive("changeItemList", function($compile, $stateParams) {
    return {
        restrict: 'A',
        template: "<ul class='list-group'>" +
            "<li class='list-group-item filled_items' ng-repeat='item in items.list | objectToArray | orderBy:\"title\"'>" +
            "<a ui-sref='problems.idea({id: problem.id, ideaId: item.id})'>{{item.title}}</a>" +
            "<button class='btn btn-danger' ng-click='items.remove($index)'><i class='fa fa-minus'></i></button>" +
            "</li>" +
            "<li class='list-group-item filled_items'>" +
            "<form ng-submit='items.add()'>" +
            "<input type='text' ng-model='items.new.title' required>" +
            "<button submit-on-click class='btn btn-success'><i class='fa fa-plus'></i></button>" +
            "</form>" +
            "</li>" +
            "</ul>",
        scope: {
            items: '=',
            problem: '='
        },
        link: function(scope, element, attrs) {
        }
    };
});
