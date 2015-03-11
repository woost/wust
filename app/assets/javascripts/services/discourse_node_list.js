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
                toastr.success("Added new node");
                self.list.push(data);
                self.new.title = "";
            });
        };
    }

    function disconnect(disconnectFunc) {
        return function(elem) {
            var self = this;
            var index = self.list.indexOf(elem);
            disconnectFunc(self.id, elem.id).$promise.then(function(data) {
                toastr.success("Disconnected node");
                self.list.splice(index, 1);
            });
        };
    }

    function onSearchSelect($item) {
        //TODO: this adds a new item, should just connect to an existing one:
        this.new = $item;
        this.add();
    }

    function Node(id, queryFunc, createFunc, disconnectFunc) {
        this.id = id;
        this.new = {
            title: ""
        };
        this.list = queryFunc ? queryFunc(this.id) : [];
        this.add = createFunc ? add(createFunc).bind(this) : todo;
        this.disconnect = disconnectFunc ? disconnect(disconnectFunc).bind(this) : todo;
        this.onSelect = onSearchSelect.bind(this);
    }

    function Goal() {
        Node.apply(this, arguments);
        this.search = Search.queryGoals;
        this.info = DiscourseNode.goal;
        this.style = {
            templateUrl: "show_discourse_node_list.html",
            listCss: "discourse_node_list",
        };
    }

    function Problem() {
        Node.apply(this, arguments);
        this.search = Search.queryProblems;
        this.info = DiscourseNode.problem;
        this.style = {
            templateUrl: "show_discourse_node_list.html",
            listCss: "discourse_node_list",
        };
    }

    function Idea() {
        Node.apply(this, arguments);
        this.search = Search.queryIdeas;
        this.info = DiscourseNode.idea;
        this.style = {
            templateUrl: "show_discourse_idea_list.html",
            listCss: "discourse_idea_list",
        };
    }
});
