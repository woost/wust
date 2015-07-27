angular.module("wust.components").controller("PageCtrl", PageCtrl);

PageCtrl.$inject = ["LeftSideService"];

function PageCtrl(LeftSideService) {
    let vm = this;

    vm.leftSide = LeftSideService;
}
