angular.module("wust").directive('visNetwork', function() {
    return {
        restrict: 'A',
        require: '^ngModel',
        scope: {
            ngModel: '=',
            onClick: '&',
            options: '='
        },
        link: function(scope, element) {
            var network = new vis.Network(element[0], scope.ngModel, scope.options || {});
            var onClick = scope.onClick() || function(prop) {};
            network.on('click', function(properties) {
                onClick(properties);
            });
        }
    };
});
