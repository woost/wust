app.service("NodeHistory", function(DiscourseNode, Utils) {
    var maximum = 8;
    var visited = [];
    this.visited = visited;
    this.add = add;
    this.remove = remove;

    function remove(id) {
        Utils.removeElementBy(visited, function(item) {
            return id === item.node.id;
        });
    }

    function add(node) {
        node.$promise.then(function(node) {
            var obj = {
                node: node,
                info: DiscourseNode.get(node.label)
            };

            remove(node.id);
            visited.push(obj);
            if (visited.length > maximum) {
                visited.splice(0, visited.length - maximum);
            }
        });

    }
});
