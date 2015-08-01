angular.module("wust.components").controller("NewStreamCtrl", NewStreamCtrl);

NewStreamCtrl.$inject = ["Search", "DiscourseNode", "StreamService"];

function NewStreamCtrl(Search, DiscourseNode, StreamService) {
    let vm = this;

    vm.save = save;
    vm.nodeInfo = DiscourseNode.Tag;
    vm.selectedTags = [];

    function save() {
        StreamService.push(vm.selectedTags);
    }
}
