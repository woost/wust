angular.module("wust.components").controller("ZenCtrl", ZenCtrl);

ZenCtrl.$inject = ["ZenService", "$rootScope"];

function ZenCtrl(ZenService, $rootScope) {
    let vm = this;

    vm.zen = ZenService;
}
