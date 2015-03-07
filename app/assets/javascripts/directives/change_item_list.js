app.directive("changeItemList", function($compile) {
    return {
        restrict: 'A',
        template: "<ul class='list-group'>" +
            "<li class='list-group-item filled_items' ng-repeat='item in items.list | objectToArray | orderBy:\"title\"'>" +
            "<span ng-bind='item.title'></span>" +
            "<button class='btn btn-danger' ng-click='items.remove()($index)'><i class='fa fa-minus'></i></button>" +
            "</li>" +
            "<li class='list-group-item filled_items'>" +
            "<form ng-submit='items.add()()'>" +
            "<input type='text' ng-model='items.new.title'>" +
            "<button submit-on-click class='btn btn-success'><i class='fa fa-plus'></i></button>" +
            "</form>" +
            "</li>" +
            "</ul>",
        scope: {
            items: '=',
        },
        link: function(scope, element, attrs) {
        }
    };
});
