angular.module("wust.elements").directive("preview", preview);

preview.$inject = [];

function preview() {
    return {
        restrict: "A",
        replace: true,
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

    $scope.$watch("vm.node.title", updateDisplayedProperties);
    $scope.$watch("vm.node.description", updateDisplayedProperties);

    function updateDisplayedProperties() {
        if (vm.node === undefined || vm.node.title === undefined || vm.node.description === undefined)
            vm.displayTitle = false;
        else
            vm.displayTitle = vm.node.title.slice(0, 137) !== vm.node.description.slice(0, 137);
    }
}
