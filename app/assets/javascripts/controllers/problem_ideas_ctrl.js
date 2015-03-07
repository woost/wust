app.controller('ProblemIdeasCtrl', function($scope, $stateParams, Idea) {
    $scope.idea = Idea.get($stateParams.ideaId);
});
