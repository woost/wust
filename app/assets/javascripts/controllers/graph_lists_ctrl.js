app.controller('GraphListsCtrl', function($scope, $state, $filter, Graph, initialData) {
    $scope.$watch('search.label', filter);

    var nodes = new vis.DataSet();
    var edges = new vis.DataSet();
    nodes.add(initialData.nodes);
    edges.add(initialData.edges);
    $scope.data = {
        graph: {
            nodes: nodes,
            edges: edges
        },
        options: {
            navigation: true,
            dataManipulation: true,
            nodes: {
                shape: 'box',
                mass: 1.2
            },
            edges: {
                style: 'arrow'
            },
        },
        onSelect: onSelect
    };

    var newProblem = {
        label: "PROBLEM",
        text: ""
    };
    $scope.addProblem = addProblem;

    function addProblem() {
        var obj = {
            "node": angular.copy(newProblem)
        };

        Graph.create(obj).$promise.then(function(data) {
            nodes.add(data.node);
            toastr.success("Created new Problem");
        }, function(response) {
            toastr.error("Failed to create Problem");
        });

        newProblem.text = "";
    }

    function onSelect(properties) {
        var id = properties.nodes[0];
        if (id === undefined) {
            return;
        }

        $state.go('graphs.detail', {
            id: id
        });
    }

    function filter() {
        var filtered = $filter('filter')(initialData.nodes, $scope.search);
        nodes.update(filtered);
        nodes.forEach(function(node) {
            if (!filtered.find(function(n) { return n.id === node.id; })) {
                nodes.remove(node.id);
            }
        });
    }
});
