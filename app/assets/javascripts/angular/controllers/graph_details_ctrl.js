app.controller('GraphDetailsCtrl', function($scope, $stateParams, initialData) {
    $scope.ideas = initialData.ideas;
    $scope.questions = initialData.questions;
});
