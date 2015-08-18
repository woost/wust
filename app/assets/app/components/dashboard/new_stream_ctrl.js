// TODO: please delete me
angular.module("wust.components").controller("NewStreamCtrl", NewStreamCtrl);

NewStreamCtrl.$inject = ["Search", "StreamService"];

function NewStreamCtrl(Search, StreamService) {
    let vm = this;

    vm.save = save;

    let currentEditStream = StreamService.currentEditStream;
    StreamService.currentEditStream = undefined;
    if (currentEditStream) {
        vm.selectedTags = angular.copy(currentEditStream.tags);
    } else {
        vm.selectedTags = [];
    }

    function save() {
        if (currentEditStream) {
            currentEditStream.tags = vm.selectedTags;
            StreamService.refreshStream(currentEditStream);
        } else {
            StreamService.push(vm.selectedTags);
        }
    }
}
