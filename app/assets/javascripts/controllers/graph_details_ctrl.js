app.controller('GraphDetailsCtrl', function($scope, $stateParams, Graph) {
    $scope.selected = Graph.get($stateParams.id);
});
