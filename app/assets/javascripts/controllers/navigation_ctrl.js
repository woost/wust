angular.module("wust").controller("NavigationCtrl", function($scope, Search, DiscourseNode, $state) {
    $scope.searchTyped = {
        title: ""
    };

    $scope.searchNodes = searchNodes;
    $scope.onSelect = onSelect;

    function searchNodes(title) {
        return Search.$search({title: title});
    }

    function onSelect($item) {
        let state = DiscourseNode.get($item.label).state;
        $state.go(state, {
            id: $item.id
        });
        $scope.searchTyped.title = "";
    }
});
