angular.module("wust.components").controller("GraphsCtrl", GraphsCtrl);

GraphsCtrl.$inject = ["$scope", "$filter", "Graph", "DiscourseNode"];

function GraphsCtrl($scope, $filter, Graph, DiscourseNode) {
    let vm = this;

    vm.onClick = onClick;
    vm.search = {
        title: ""
    };

    vm.graph = {};
    Graph.$fetch().$then(createGraph);

    $scope.$watch("vm.search.title", filter);

    function createGraph(graph) {
        graph.nodes = graph.nodes.map(node => _.merge(node, {
            css: node.hyperEdge ? "" : `node ${DiscourseNode.get(node.label).css}`
        }));
        vm.graph = graph;
    }

    function filter() {
        let filtered = $filter("fuzzyFilter")(_.filter(vm.graph.nodes, { hyperEdge: false }), vm.search);
        $scope.$broadcast("d3graph_filter", filtered);
    }

    function onClick(d) {
        DiscourseNode.get(d.label).gotoState(d.id);
    }
}
