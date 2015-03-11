app.service('DiscourseNodeList', function(DiscourseNode, Search) {
    this.Goal = Goal;
    this.Problem = Problem;
    this.Idea = Idea;

    function todo() {
        toastr.error("not implemented");
    }

    function add(createFunc) {
        return function() {
            var self = this;
            createFunc(self.id, self.new).$promise.then(function(data) {
                toastr.success("Added new item");
                self.list.push(data);
                self.new.title = "";
            });
        };
    }

    function remove(removeFunc) {
        return function(elem) {
            var self = this;
            var index = self.list.indexOf(elem);
            removeFunc(elem.id).$promise.then(function(data) {
                toastr.success("Removed item");
                self.list.splice(index, 1);
            });
        };
    }

    function onSearchSelect($item) {
        //TODO: this adds a new item, should just connect to an existing one:
        this.new = $item;
        this.add();
    }

    function Item(id, queryFunc, createFunc, removeFunc) {
        this.id = id;
        this.new = {
            title: ""
        };
        this.list = queryFunc ? queryFunc(this.id) : [];
        this.add = createFunc ? add(createFunc).bind(this) : todo;
        this.remove = removeFunc ? remove(removeFunc).bind(this) : todo;
        this.onSelect = onSearchSelect.bind(this);
    }

    function Goal() {
        Item.apply(this, arguments);
        this.search = Search.queryGoals;
        this.info = DiscourseNode.problem;
        this.style = {
            templateurl: "show_discourse_node_list.html",
            listcss: "discourse_node_list",
        };
    }

    function Problem() {
        Item.apply(this, arguments);
        this.search = Search.queryProblems;
        this.info = DiscourseNode.problem;
        this.style = {
            templateUrl: "show_discourse_node_list.html",
            listCss: "discourse_node_list",
        };
    }

    function Idea() {
        Item.apply(this, arguments);
        this.search = Search.queryIdeas;
        this.info = DiscourseNode.idea;
        this.style = {
            templateUrl: "show_discourse_idea_list.html",
            listCss: "discourse_idea_list",
        };
    }
});
