app.controller('SearchCtrl', function($scope, Search, DiscourseNode, $state) {
    $scope.searchTyped = {
        title: ""
    };

    $scope.searchNodes = Search.query;
    $scope.onSelect = onSelect;

    function onSelect($item) {
        var state = DiscourseNode.getState($item.label);
        $state.go(state, {
            id: $item.id
        });
        $scope.searchTyped.title = "";
    }
});
