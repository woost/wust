app.controller('GraphDetailsCtrl', function($scope, $stateParams, initialData) {
    $scope.selected = {
        title: initialData.node.type + ': ' + initialData.node.label,
        ideas: initialData.ideas,
        questions: initialData.questions
    };

    $scope.newIdea = {
        id: 666,
        type: 'idea',
        label: ''
    };

    $scope.newQuestion = {
        id: 666,
        type: 'question',
        label: ''
    };

    $scope.addNode = addNode;
    $scope.removeNode = removeNode;

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
