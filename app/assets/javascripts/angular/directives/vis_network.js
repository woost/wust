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
            var nodes = new vis.DataSet();
            var edges = new vis.DataSet();
            nodes.add($scope.ngModel.nodes);
            edges.add($scope.ngModel.edges);
            var graph = {
                nodes: nodes,
                edges: edges
            };

            var network = new vis.Network($element[0], graph, $scope.options || {});
            var onSelect = $scope.onSelect() || function(prop) {};
            network.on('select', function(properties) {
                onSelect(properties);
            });

        }

    }
});
