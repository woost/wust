angular.module("wust.elements").controller("ModalEditCtrl", ModalEditCtrl);

ModalEditCtrl.$inject = ["DiscourseNode", "Search", "EditService", "ModalEditService"];

function ModalEditCtrl(DiscourseNode, Search, EditService,ModalEditService) {
    let vm = this;

    vm.hasFocus = true;
    vm.previewEnabled = false;
    vm.node = ModalEditService.currentNode;

    vm.save = save;

    function save() {
        ModalEditService.save();
        ModalEditService.hide();
    }
}
