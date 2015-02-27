app.controller('GraphDetailsCtrl', function($scope, $stateParams) {
    $scope.graph = $scope.graphs.find(function(element) {
        return element.id == $stateParams.id;
    });
});
