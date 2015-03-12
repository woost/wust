app.service('DiscourseNodeList', function(DiscourseNode, Idea, Problem, Goal, Search) {
    this.Goal = GoalNode;
    this.Problem = ProblemNode;
    this.Idea = IdeaNode;

    function todo() {
        toastr.error("not implemented");
    }

    function create(createFunc) {
        return function() {
            var self = this;
            createFunc(self.new).$promise.then(function(data) {
                toastr.success("Created new node");
                self.add(data);
            });
        };
    }

    function remove(disconnectFunc) {
        return function(elem) {
            var self = this;
            var index = self.list.indexOf(elem);
            disconnectFunc(self.id, elem.id).$promise.then(function(data) {
                toastr.success("Disconnected node");
                self.list.splice(index, 1);
            });
        };
    }

    function add(connectFunc) {
        return function($item) {
            var self = this;
            connectFunc(self.id, $item.id).$promise.then(function(data) {
                toastr.success("Connected node");
                self.list.push(data);
                self.new.title = "";
            });
        };
    }

    function Node(id, queryFunc, connectFunc, disconnectFunc) {
        this.id = id;
        this.new = {
            title: ""
        };
        this.list = queryFunc ? queryFunc(this.id) : [];
        this.remove = disconnectFunc ? remove(disconnectFunc).bind(this) : todo;
        this.add = connectFunc ? add(connectFunc).bind(this) : todo;
    }

    function GoalNode() {
        Node.apply(this, arguments);
        this.create = create(Goal.create).bind(this);
        this.search = Search.queryGoals;
        this.info = DiscourseNode.goal;
        this.style = {
            templateUrl: "show_discourse_node_list.html",
            listCss: "discourse_node_list",
        };
    }

    function ProblemNode() {
        Node.apply(this, arguments);
        this.create = create(Problem.create).bind(this);
        this.search = Search.queryProblems;
        this.info = DiscourseNode.problem;
        this.style = {
            templateUrl: "show_discourse_node_list.html",
            listCss: "discourse_node_list",
        };
    }

    function IdeaNode() {
        Node.apply(this, arguments);
        this.create = create(Idea.create).bind(this);
        this.search = Search.queryIdeas;
        this.info = DiscourseNode.idea;
        this.style = {
            templateUrl: "show_discourse_idea_list.html",
            listCss: "discourse_idea_list",
        };
    }
});
