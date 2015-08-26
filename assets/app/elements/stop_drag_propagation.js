angular.module("wust.elements").directive("stopDragPropagation", stopDragPropagation);

stopDragPropagation.$inject = [];

function stopDragPropagation() {
    return {
        restrict: "A",
        link
    };

    function link(scope, elem) {
        elem.on("dragstart", e => e.stopPropagation());
    }
}
