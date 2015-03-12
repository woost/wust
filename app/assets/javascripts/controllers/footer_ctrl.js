app.controller('FooterCtrl', function($scope, StateHistory) {
    $scope.states = StateHistory.states;
});
