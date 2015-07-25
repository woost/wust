angular.module("wust.elements").directive("d3Relation", d3Relation);

d3Relation.$inject = [];

function d3Relation() {
    return {
        restrict: "A",
        templateUrl: "assets/app/graph/d3_graph/d3_relation.html",
        replace: true,
        link: function(scope, element) {
            scope.relation.domRelation = element[0];
            if (scope.$last === true)
                scope.$emit("d3_relation_last");
        }
    };
}
