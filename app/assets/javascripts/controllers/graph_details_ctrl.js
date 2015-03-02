app.controller('GraphDetailsCtrl', function($scope, $stateParams, Graph, initialData) {
    $scope.selected = {
        title: initialData.node.label + ': ' + initialData.node.text,
        ideas: initialData.ideas,
        questions: initialData.questions
    };

    $scope.newIdea = {
        relationLabel: 'SOLVES',
        label: 'IDEA',
        text: ''
    };

    $scope.newQuestion = {
        relationLabel: 'ASKS',
        label: 'QUESTION',
        text: ''
    };

    $scope.addNode = addNode;
    $scope.removeNode = removeNode;

    function addNode(list, elem) {
        var obj = {
            "reference": $stateParams.id,
            "label": elem.relationLabel,
            "node": {
                "label": elem.label,
                "text": elem.text
            }
        };

        Graph.create(obj).$promise.then(function (data) {
            list.push(data.node);
            $scope.data.graph.nodes.add(data.node);
            $scope.data.graph.edges.add(data.relation);
            toastr.success("Created new Node");
        }, function(response) {
            toastr.error("Failed to create Node");
        });

        elem.text = '';
    }

    function removeNode(list, elem) {
        var index = list.indexOf(elem);
        if (index < 0) {
            return;
        }

        list.splice(index, 1);
    }
});
