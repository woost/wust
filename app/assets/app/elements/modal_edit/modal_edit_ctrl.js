angular.module("wust.components").controller("ModalEditCtrl", ModalEditCtrl);

ModalEditCtrl.$inject = ["ModalEditService"];

function ModalEditCtrl(ModalEditService) {
    let vm = this;

    vm.previewEnabled = false;
    vm.node = ModalEditService.currentNode;
    vm.reference = ModalEditService.reference;
    vm.save = ModalEditService.save;
}
