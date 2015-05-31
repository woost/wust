angular.module("wust.components").controller("PostsCtrl", PostsCtrl);

PostsCtrl.$inject = ["$stateParams", "Post", "DiscourseNode", "DiscourseNodeList", "DiscourseNodeCrate"];

function PostsCtrl($stateParams, Post, DiscourseNode, DiscourseNodeList, DiscourseNodeCrate) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Post;
    let node = Post.$find($stateParams.id);
    vm.node = DiscourseNodeCrate(node);
    vm.top = DiscourseNodeList.read.Post(node.connectsFrom.$search(), "From")
        .nested(DiscourseNodeList.read.Post, "connectsFrom", "From")
        .nested(DiscourseNodeList.read.Post, "connectsTo", "To");
    vm.bottom = DiscourseNodeList.read.Post(node.connectsTo.$search(), "To")
        .nested(DiscourseNodeList.read.Post, "connectsFrom", "From")
        .nested(DiscourseNodeList.read.Post, "connectsTo", "To");
}
