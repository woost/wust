app.directive("changeItemList", function($compile) {
    return {
        restrict: 'A',
        template: "<ul class='list-group'>" +
            "<li class='list-group-item' ng-repeat='item in items'>" +
            "<button class='btn btn-danger' ng-click='removeItem()(items, item)'><i class='fa fa-minus'></i></button>" +
            "<span ng-bind='item.label'></span>" +
            "</li>" +
            "<li class='list-group-item'>" +
            "<button class='btn btn-success' ng-click='addItem()(items, newItem)'><i class='fa fa-plus'></i></button>" +
            "<input type='text' ng-model='newItem.label'>" +
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
