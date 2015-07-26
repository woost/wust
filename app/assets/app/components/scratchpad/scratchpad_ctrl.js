angular.module("wust.components").controller("ScratchpadCtrl", ScratchpadCtrl);

ScratchpadCtrl.$inject = ["EditService", "LeftSideService"];

function ScratchpadCtrl(EditService, LeftSideService) {
    let vm = this;

    vm.leftSide = LeftSideService;
    vm.editList = EditService.list;
    vm.edit = EditService.edit;
}
