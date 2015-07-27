angular.module("wust.components").directive("zen", zen);

zen.$inject = [];

function zen() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/zen/zen.html",
        scope: true,
        controller: zenCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

zenCtrl.$inject = ["ZenService", "$rootScope"];

function zenCtrl(ZenService, $rootScope) {
    let vm = this;

    vm.zen = ZenService;
}
