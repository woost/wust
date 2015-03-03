app.directive("changeItemList", function($compile) {
    return {
        restrict: 'A',
        template: "<ul class='list-group'>" +
            "<li class='list-group-item filled_items' ng-repeat='item in items | objectToArray | orderBy:\"label\"'>" +
            "<a ui-sref='graphs.detail({id: item.id})' ng-bind='item.label'></a>" +
            "<button class='btn btn-danger' ng-click='removeItem()($index)'><i class='fa fa-minus'></i></button>" +
            "</li>" +
            "<li class='list-group-item filled_items'>" +
            "<input type='text' ng-model='newItem.label'>" +
            "<button class='btn btn-success' ng-click='addItem()(newItem)'><i class='fa fa-plus'></i></button>" +
            "</li>" +
            "</ul>",
        replace: true,
        scope: {
            items: '=',
            newItem: '=',
            addItem: '&',
            removeItem: '&'
        },
        link: function(scope, element, attrs) {
        }
    };
});
