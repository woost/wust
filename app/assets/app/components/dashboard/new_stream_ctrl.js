angular.module("wust.components").controller("NewStreamCtrl", NewStreamCtrl);

NewStreamCtrl.$inject = ["$scope", "$modalInstance", "items", "Search", "DiscourseNode"];

function NewStreamCtrl($scope, $modalInstance, items, Search, DiscourseNode) {
    let vm = this;
    vm.ok = ok;
    vm.cancel = cancel;

    vm.searchTags = searchTags;
    vm.selectTag = selectTag;
    vm.nodeInfo = DiscourseNode.Tag;
    vm.selectedTags = [];

    function ok(selectedTags) {
        $modalInstance.close(selectedTags);
    }

    function cancel() {
        $modalInstance.dismiss("cancel");
    }


    function selectTag(tag) {
        //TODO: possibility to create new tags
        vm.selectedTags.push(tag);
    }

    function searchTags(title) {
        return Search.$search({
            title: title,
            label: DiscourseNode.Tag.label
        });
    }
}
