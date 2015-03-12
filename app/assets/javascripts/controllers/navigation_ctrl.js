app.controller('NavigationCtrl', function($scope, Search, DiscourseNode, $state) {
    $scope.searchTyped = {
        title: ""
    };

    $scope.searchNodes = Search.query;
    $scope.onSelect = onSelect;

    function onSelect($item) {
        var state = DiscourseNode.get($item.label).state;
        $state.go(state, {
            id: $item.id
        });
        $scope.searchTyped.title = "";
    }
});
