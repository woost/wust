app.controller('GraphDetailsCtrl', function($scope, $stateParams, initialData) {
    $scope.ideas = initialData.ideas;
    $scope.questions = initialData.questions;
    $scope.addNode = addNode;
    $scope.removeNode = removeNode;
    $scope.newIdea = {
        type: 'idea',
        label: ''
    };
    $scope.newQuestion = {
        type: 'question',
        label: ''
    };

    function addNode(list, elem) {
        list.push(angular.copy(elem));
        elem.label = '';
    }

    function removeNode(list, elem) {
        var index = list.indexOf(elem);
        if (index < 0) {
            return;
        }

        list.splice(index, 1);
    }
});
