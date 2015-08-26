angular.module("wust.components").directive("focusGraph", focusGraph);

focusGraph.$inject = [];

function focusGraph() {
    return {
        restrict: "A",
        templateUrl: "components/focus/graph/graph.html",
        scope: {
            component: "=",
            isLoading: "="
        },
        controller: GraphsCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

GraphsCtrl.$inject = [];

function GraphsCtrl() {
    let vm = this;
}
