angular.module("wust.elements").controller("ModalEditCtrl", ModalEditCtrl);

ModalEditCtrl.$inject = ["ModalEditService"];

function ModalEditCtrl(ModalEditService) {
    let vm = this;

    vm.hasFocus = true;
    vm.previewEnabled = false;
    vm.node = ModalEditService.currentNode;

    vm.save = save;

    function save() {
        if (vm.tagSearch) {
            vm.node.tags.push({title: vm.tagSearch});
            vm.tagSearch = "";
        }

        ModalEditService.save();
    }
}
