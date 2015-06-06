angular.module("wust.components").controller("ScratchpadCtrl", ScratchpadCtrl);

ScratchpadCtrl.$inject = ["EditService", "Search", "DiscourseNode"];

function ScratchpadCtrl(EditService, Search, DiscourseNode) {
    let vm = this;

    vm.searchNodes = searchNodes;
    vm.nodeInfo = DiscourseNode.Post;
    vm.editStack = EditService.stack;
    vm.edit = EditService.edit;

    function searchNodes(title) {
        return Search.$search({
            title: title,
            label: DiscourseNode.Post.label
        });
    }
}
