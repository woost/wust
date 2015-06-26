angular.module("wust.components").controller("NewStreamCtrl", NewStreamCtrl);

NewStreamCtrl.$inject = ["$scope", "$modalInstance", "items", "Search", "DiscourseNode"];

function NewStreamCtrl($scope, $modalInstance, items, Search, DiscourseNode) {
    let vm = this;
    vm.ok = function(selectedTags) {
        $modalInstance.close(selectedTags);
    };

    vm.cancel = function() {
        $modalInstance.dismiss("cancel");
    };

    vm.nodeInfo = DiscourseNode.Tag;

    vm.searchTags = searchTags;
    vm.selectTag = selectTag;
    vm.selectedTags = [];

    function selectTag(tag) {
        vm.selectedTags.push(tag);
    }

    function searchTags(title) {
        return Search.$search({
            title: title,
            label: DiscourseNode.Tag.label
        });
    }
}
