app.service('Problem', function(DiscourseNode, Node) {
    return new Node(DiscourseNode.problem.state);
});
