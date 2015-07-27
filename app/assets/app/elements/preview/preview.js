angular.module("wust.elements").directive("preview", preview);

preview.$inject = [];

function preview() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "assets/app/elements/preview/preview.html",
        scope: {
            node: "="
        },
        controller: previewCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

previewCtrl.$inject = [];

function previewCtrl() {
    let vm = this;
}
