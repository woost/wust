angular.module("wust.components").controller("ScratchpadCtrl", ScratchpadCtrl);

ScratchpadCtrl.$inject = ["EditService", "DiscourseNode"];

function ScratchpadCtrl(EditService, DiscourseNode) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Post;
    vm.editList = EditService.list;
    vm.edit = EditService.edit;
}
