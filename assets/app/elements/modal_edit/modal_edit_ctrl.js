angular.module("wust.elements").controller("ModalEditCtrl", ModalEditCtrl);

ModalEditCtrl.$inject = ["DiscourseNode", "Search", "EditService", "ModalEditService"];

function ModalEditCtrl(DiscourseNode, Search, EditService,ModalEditService) {
    let vm = this;

    vm.hasFocus = true;
    vm.previewEnabled = false;
    vm.node = ModalEditService.currentNode;

    vm.save = save;

    function save(callback) {
        return vm.node.referenceNode ? ModalEditService.save(callback) : ModalEditService.save().$then(callback);
    }
}
