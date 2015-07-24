angular.module("wust.components").controller("NewStreamCtrl", NewStreamCtrl);

NewStreamCtrl.$inject = ["$scope", "Search", "DiscourseNode", "StreamService"];

function NewStreamCtrl($scope, Search, DiscourseNode, StreamService) {
    let vm = this;

    vm.save = save;
    vm.searchTags = searchTags;
    vm.selectTag = selectTag;
    vm.nodeInfo = DiscourseNode.Tag;
    vm.selectedTags = [];

    function save() {
        StreamService.push(vm.selectedTags);
    }

    function selectTag(tag) {
        //TODO: possibility to create new tags
        // afaik it does not make sense to create tags when you are creating a stream
        vm.selectedTags.push(tag);
    }

    function searchTags(title) {
        return Search.$search({
            title: title,
            label: DiscourseNode.Tag.label
        });
    }
}
