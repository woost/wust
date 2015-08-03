angular.module("wust.components").controller("NewStreamCtrl", NewStreamCtrl);

NewStreamCtrl.$inject = ["Search", "DiscourseNode", "StreamService"];

function NewStreamCtrl(Search, DiscourseNode, StreamService) {
    let vm = this;

    vm.save = save;
    vm.nodeInfo = DiscourseNode.Tag;

    if (StreamService.currentEditStream) {
        vm.selectedTags = angular.copy(StreamService.currentEditStream.tags);
    } else {
        vm.selectedTags = [];
    }

    function save() {
        if (StreamService.currentEditStream) {
            StreamService.refreshEditStream(vm.selectedTags);
        } else {
            StreamService.push(vm.selectedTags);
        }
    }
}
