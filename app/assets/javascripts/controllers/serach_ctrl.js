app.controller('SearchCtrl', function($scope, Search, $state) {
    $scope.selected = undefined;

    $scope.getNodes = getNodes;
    $scope.onSelect = onSelect;

    function getNodes(term) {
        return Search(term).$promise.then(function(response) {
            return response.map(function(item) {
                return item;
            });
        });
    }

    function onSelect($item, $model, $label) {
        $state.go("problems", {
            id: $item.id
        });
        $scope.searchSelected = "";
    }
});
