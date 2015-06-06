angular.module("wust.components").directive("focusGraph", focusGraph);

focusGraph.$inject = [];

function focusGraph() {
    return {
        restrict: "A",
        templateUrl: "assets/app/components/focus/graph/graph.html",
        scope: {
            component: "=",
            rootId: "="
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
