angular.module("wust").service('Goal', function(DiscourseNode, Node) {
    return new Node(DiscourseNode.goal.state);
});
