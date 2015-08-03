angular.module("wust.components").controller("NewStreamCtrl", NewStreamCtrl);

NewStreamCtrl.$inject = ["Search", "DiscourseNode", "StreamService"];

function NewStreamCtrl(Search, DiscourseNode, StreamService) {
    let vm = this;

    vm.save = save;
    vm.nodeInfo = DiscourseNode.Tag;

    let currentEditStream = StreamService.currentEditStream;
    StreamService.currentEditStream = undefined;
    if (currentEditStream) {
        vm.selectedTags = angular.copy(currentEditStream.tags);
    } else {
        vm.selectedTags = [];
    }

    function save() {
        if (currentEditStream) {
            StreamService.refreshStream(currentEditStream, vm.selectedTags);
        } else {
            StreamService.push(vm.selectedTags);
        }
    }
}
