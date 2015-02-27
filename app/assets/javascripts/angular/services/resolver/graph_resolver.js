app.factory("GraphResolver", function(Graph, $q) {
    return function(id) {
        return Graph.get(id).$promise;
    };
});
