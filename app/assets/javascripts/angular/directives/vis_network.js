app.directive('visNetwork', function() {
    return {
        restrict: 'EA',
        require: '^ngModel',
        scope: {
            ngModel: '=',
            onSelect: '&',
            options: '='
        },
        link: function($scope, $element, $attrs, ngModel) {
            var network = new vis.Network($element[0], $scope.ngModel, $scope.options || {});
            var onSelect = $scope.onSelect() || function(prop) {};
            network.on('select', function(properties) {
                onSelect(properties);
            });
        }
    };
});
