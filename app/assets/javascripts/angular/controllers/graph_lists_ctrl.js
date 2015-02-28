app.controller('GraphListsCtrl', function($scope, $state, initialData) {
    $scope.graph = initialData.graph;
    $scope.onSelect = onSelect;

    function onSelect(properties) {
        var id = properties.nodes[0];
        if (id === undefined) {
            return;
        }

        $state.go('graphs.detail', { id: id });
    }
});
