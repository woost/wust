angular.module("wust.elements").directive("d3Node", d3Node);

d3Node.$inject = [];

function d3Node() {
    return {
        restrict: "A",
        templateUrl: "assets/app/graph/d3_graph/d3_node.html",
        replace: true,
        link: function(scope, element) {
            console.log("link: " + scope.node.id);
            //TODO: select by name
            scope.node.domNodeFrame = element[0];
            scope.node.domNode = element[0].children[0];
            if (scope.$last === true) {
                console.log("node: scope.$last");
                scope.$emit("d3_node_last");
            }
        }
    };
}
