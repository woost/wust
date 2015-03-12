app.service("NodeHistory", function(DiscourseNode, Utils) {
    var maximum = 8;
    var visited = [];
    this.visited = visited;
    this.add = add;

    function add(node) {
        node.$promise.then(function(data) {
            var obj = {
                node: data,
                info: DiscourseNode.get(data.label)
            };

            Utils.removeElementBy(visited, function(item) {
                return data.id === item.node.id;
            });

            visited.push(obj);
            if (visited.length > maximum) {
                visited.splice(0, visited.length - maximum);
            }
        });

    }
});
