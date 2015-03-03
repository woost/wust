app.controller('GraphsCtrl', function($scope, $state, $filter, Graph, Utils, initialData) {
    $scope.$watch('search.label', filter);

    $scope.addProblem = addProblem;
    $scope.newProblem = {
        label: "",
        text: ""
    };

    var nodes = new vis.DataSet();
    var edges = new vis.DataSet();
    nodes.add(initialData.nodes);
    edges.add(initialData.edges);

    $scope.data = {
        addNode: addNode,
        addEdge: addEdge,
        removeNode: removeNode,
        removeEdge: removeEdge,
        graph: {
            nodes: nodes,
            edges: edges
        },
        options: {
            navigation: true,
            dataManipulation: false,
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

    function addProblem() {
        var obj = {
            "node": {
                type: "PROBLEM",
                label: $scope.newProblem.label,
                text: $scope.newProblem.text
            }
        };

        Graph.create(obj).$promise.then(function(data) {
            addNode(data.node);
            toastr.success("Created new Problem");
        }, function(response) {
            toastr.error("Failed to create Problem");
        });

        $scope.newProblem.label = "";
        $scope.newProblem.text = "";
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
        angular.forEach(nodes, function(node) {
            if ($filter('filter')(filtered, {
                id: node.id
            }).length === 0) {
                nodes.remove(node.id);
            }
        });
    }

    function addNode(node) {
        initialData.nodes.push(node);
        nodes.add(node);
        filter();
    }

    function removeNode(node) {
        Utils.removeElementBy(initialData.nodes, function(n) {
            return n.id === node.id;
        });
        nodes.remove(node);
    }

    function addEdge(edge) {
        edges.add(edge);
    }

    function removeEdge(edge) {
        edges.remove(edge.id);
    }
});
