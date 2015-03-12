app.service('Idea', function(DiscourseNode, Node) {
    return new Node(DiscourseNode.idea.state);
});
