angular.module("wust.components").controller("PostsCtrl", PostsCtrl);

PostsCtrl.$inject = ["$stateParams", "Post", "DiscourseNode", "DiscourseNodeList", "DiscourseNodeCrate"];

function PostsCtrl($stateParams, Post, DiscourseNode, DiscourseNodeList, DiscourseNodeCrate) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.Post;
    let node = Post.$find($stateParams.id);
    vm.node = DiscourseNodeCrate(node);
    vm.top = DiscourseNodeList.write.Post(node.connectsFrom.$search(), "From")
        .nested(DiscourseNodeList.write.Post, "connectsFrom", "From")
        .nested(DiscourseNodeList.write.Post, "connectsTo", "To");
    vm.bottom = DiscourseNodeList.write.Post(node.connectsTo.$search(), "To")
        .nested(DiscourseNodeList.write.Post, "connectsFrom", "From")
        .nested(DiscourseNodeList.write.Post, "connectsTo", "To");
}
