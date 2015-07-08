angular.module("wust.graph").directive("graphView", graphView);

graphView.$inject = [];

function graphView() {
    return {
        restrict: "A",
        templateUrl: "assets/app/graph/graph_view/graph_view.html",
        replace: true,
        scope: {
            graph: "="
        },
        controller: graphViewCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

graphViewCtrl.$inject = ["$scope", "DiscourseNode", "$filter"];

function graphViewCtrl($scope, DiscourseNode, $filter) {
    let vm = this;

    vm.onClick = onClick;
    vm.search = {
        title: ""
    };

    vm.wrappedPromise = vm.graph.$wrappedPromise();
    vm.wrappedPromise.then(setGraph);

    let firstFilter = true;

    function setGraph(graph) {
        _.each(graph.nodes, (n) => {
            n.css = n.hyperEdge ? "relation_label" : `node ${DiscourseNode.get(n.label).css}`;
        });

        $scope.$watch("vm.search.title", _.wrap(graph, filter));
    }

    function filter(graph) {
        if (firstFilter) {
            firstFilter = false;
            return;
        }

        let filtered = $filter("fuzzyFilter")(_.reject(graph.nodes, { hyperEdge: true }), vm.search);
        $scope.$broadcast("d3graph_filter", filtered);
    }

    function onClick(node) {
        DiscourseNode.get(node.label).gotoState(node.id);
    }
}
