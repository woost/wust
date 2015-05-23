angular.module("wust.graph").directive("graphView", graphView);

graphView.$inject = [];

function graphView() {
    return {
        restrict: "A",
        templateUrl: "assets/app/graph/directives/graph_view.html",
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

    vm.graph.$then(createGraph);

    $scope.$watch("vm.search.title", filter);

    function createGraph(graph) {
        graph.nodes = graph.nodes.map(node => _.merge(node, {
            css: node.hyperEdge ? "relation_label" : `node ${DiscourseNode.get(node.label).css}`
        }));
    }

    function filter() {
        let filtered = $filter("fuzzyFilter")(_.reject(vm.graph.nodes, { hyperEdge: true }), vm.search);
        $scope.$broadcast("d3graph_filter", filtered);
    }

    function onClick(d) {
        DiscourseNode.get(d.label).gotoState(d.id);
    }
}