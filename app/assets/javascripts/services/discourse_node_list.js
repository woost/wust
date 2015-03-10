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

    // TODO: extract into service /search_ctrl
    function search(searchFunc) {
        return function(term) {
            return searchFunc(term).$promise.then(function(response) {
                return response.map(function(item) {
                    item.css = DiscourseNode.getCss(item.label);
                    return item;
                });
            });
        };
    }

    function onSearchSelect(createFunc) {
        return function($item, $model, $label) {
            //TODO: this adds a new item, should just connect to an existing one:
            this.new = $item;
            this.add();
        };
    }

    function Item(id, queryFunc, createFunc, removeFunc) {
        this.id = id;
        this.new = {
            title: ""
        };
        this.list = queryFunc ? queryFunc(this.id) : [];
        this.add = createFunc ? add(createFunc) : todo;
        this.remove = removeFunc ? remove(removeFunc) : todo;
        this.onSelect = createFunc ? onSearchSelect(createFunc) : todo;
        this.search = search(Search.query);
    }

    function Goal() {
        Item.apply(this, arguments);
        this.state = DiscourseNode.goal.state;
        this.search = search(Search.queryGoals);
        this.style = {
            template: "show_discourse_node_list.html",
            discourse: DiscourseNode.goal.css,
            list: "node_list",
        };
    }

    function Problem() {
        Item.apply(this, arguments);
        this.state = DiscourseNode.problem.state;
        this.search = search(Search.queryProblems);
        this.style = {
            template: "show_discourse_node_list.html",
            discourse: DiscourseNode.problem.css,
            list: "node_list",
        };
    }

    function Idea() {
        Item.apply(this, arguments);
        this.state = DiscourseNode.idea.state;
        this.search = search(Search.queryIdeas);
        this.style = {
            template: "show_discourse_idea_list.html",
            discourse: DiscourseNode.idea.css,
            list: "idea_list",
        };
    }
});
