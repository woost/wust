angular.module("wust").service("NodeHistory", function(DiscourseNode) {
    var maximum = 8;
    var visited = [];
    this.visited = visited;
    this.add = add;
    this.remove = remove;

    function remove(id) {
        _.remove(visited, (item) => id === item.node.id);
    }

    function add(node) {
        node.$then(function(node) {
            var obj = {
                node: node,
                info: DiscourseNode.get(node.label)
            };

            remove(node.id);
            visited.push(obj);
            visited.splice(0, visited.length - maximum);
        });
    }
});
