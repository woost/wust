angular.module("wust.elements").directive("zen", zen);

zen.$inject = [];

function zen() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "zen/zen.html",
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

    $rootScope.$on("$stateChangeStart", () => {
        vm.zen.visible = false;
    });
}
