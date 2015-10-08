angular.module("wust.elements").directive("preview", preview);

preview.$inject = [];

function preview() {
    return {
        restrict: "A",
        replace: true,
        transclude: true,
        templateUrl: "elements/preview/preview.html",
        scope: {
            node: "=",
            trim: "@"
        },
        controller: previewCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

previewCtrl.$inject = ["$scope"];

function previewCtrl($scope) {
    let vm = this;
}
