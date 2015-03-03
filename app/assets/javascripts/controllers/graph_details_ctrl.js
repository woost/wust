app.controller('GraphDetailsCtrl', function($scope, $stateParams, Graph, Utils, initialData) {
    $scope.selected = {
        title: initialData.node.type + ': ' + initialData.node.label,
        text: initialData.node.text,
        ideas: initialData.ideas,
        questions: initialData.questions
    };

    $scope.newIdea = {
        relationLabel: 'SOLVES',
        type: 'IDEA',
        label: ''
    };

    $scope.newQuestion = {
        relationLabel: 'ASKS',
        type: 'QUESTION',
        label: ''
    };

    $scope.addIdea = addItem($scope.selected.ideas);
    $scope.addQuestion = addItem($scope.selected.questions);
    $scope.removeIdea = removeItem($scope.selected.ideas);
    $scope.removeQuestion = removeItem($scope.selected.questions);

    function addItem(list) {
        return function(elem) {
            var obj = {
                "reference": $stateParams.id,
                "label": elem.relationLabel,
                "node": {
                    "type": elem.type,
                    "label": elem.label,
                    "text": ""
                }
            };

            Graph.create(obj).$promise.then(function(data) {
                list.push(data.node);
                $scope.data.addNode(data.node);
                $scope.data.addEdge(data.relation);
                toastr.success("Created new Node");
            }, function(response) {
                toastr.error("Failed to create Node");
            });

            elem.label = '';
        };
    }

    function removeItem(list) {
        return function($index) {
            var elem = list[$index];
            Graph.remove(elem.id).$promise.then(function(data) {
                list.splice($index, 1);
                $scope.data.removeNode(elem);
                toastr.success("Removed Node");
            }, function(response) {
                toastr.error("Failed to remove Node");
            });
        };
    }
});
