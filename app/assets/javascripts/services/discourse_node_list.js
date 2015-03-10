app.service('DiscourseNodeList', function(DiscourseNode) {
    this.Goal = Goal;
    this.Problem = Problem;
    this.Idea = Idea;

    function todo() {
        toastr.error("not implemented");
    }

    function add(createFunc, container) {
        return function() {
            createFunc(container.id, container.new).$promise.then(function(data) {
                toastr.success("Added new item");
                container.list.push(data);
                container.new.title = "";
            });
        };
    }

    function remove(removeFunc, container) {
        return function(elem) {
            var index = container.list.indexOf(elem);
            removeFunc(elem.id).$promise.then(function(data) {
                toastr.success("Removed item");
                container.list.splice(index, 1);
            });
        };
    }

    function Item(id, queryFunc, createFunc, removeFunc) {
        this.id = id;
        this.new = { title: "" };
        this.list = queryFunc ? queryFunc(this.id) : [];
        this.add = createFunc ? add(createFunc, this) : todo;
        this.remove = removeFunc ? remove(removeFunc, this) : todo;
    }

    function Goal() {
        Item.apply(this, arguments);
        this.state = DiscourseNode.goal.state;
        this.style = {
            template: "show_discourse_node_list.html",
            discourse: DiscourseNode.goal.css,
            list: "node_list",
        };
    }

    function Problem() {
        Item.apply(this, arguments);
        this.state = DiscourseNode.problem.state;
        this.style = {
            template: "show_discourse_node_list.html",
            discourse: DiscourseNode.problem.css,
            list: "node_list",
        };
    }

    function Idea() {
        Item.apply(this, arguments);
        this.state = DiscourseNode.idea.state;
        this.style = {
            template: "show_discourse_idea_list.html",
            discourse: DiscourseNode.idea.css,
            list: "idea_list",
        };
    }
});
