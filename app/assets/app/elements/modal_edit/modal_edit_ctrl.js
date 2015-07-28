angular.module("wust.components").controller("ModalEditCtrl", ModalEditCtrl);

ModalEditCtrl.$inject = ["ModalEditService"];

function ModalEditCtrl(ModalEditService) {
    let vm = this;

    vm.node = ModalEditService.currentNode;
    vm.save = ModalEditService.save;
}
