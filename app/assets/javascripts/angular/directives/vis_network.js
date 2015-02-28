app.directive('visNetwork', function() {
    return {
        restrict: 'A',
        require: '^ngModel',
        scope: {
            ngModel: '=',
            onSelect: '&',
            options: '='
        },
        link: function(scope, element) {
            var network = new vis.Network(element[0], scope.ngModel, scope.options || {});
            var onSelect = scope.onSelect() || function(prop) {};
            network.on('select', function(properties) {
                onSelect(properties);
            });
        }
    };
});
