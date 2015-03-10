app.controller('SearchCtrl', function($scope, Search, DiscourseNode, $state) {
    $scope.getNodes = getNodes;
    $scope.onSelect = onSelect;

    function getNodes(term) {
        return Search.query(term).$promise.then(function(response) {
            return response.map(function(item) {
                item.css = DiscourseNode.getCss(item.label);
                return item;
            });
        });
    }

    function onSelect($item, $model, $label) {
        var state = DiscourseNode.getState($item.label);
        $state.go(state, {
            id: $item.id
        });
        $scope.searchSelected = "";
    }
});
