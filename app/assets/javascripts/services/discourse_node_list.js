app.service('DiscourseNodeList', function(DiscourseNode, Idea, Problem, Goal, Search) {
    this.Goal = GoalNode;
    this.Problem = ProblemNode;
    this.Idea = IdeaNode;

    function todo() {
        toastr.error("not implemented");
    }

    function create(createFunc) {
        var self = this;
        createFunc(self.new).$promise.then(function(data) {
            toastr.success("Created new node");
            self.add(data);
        });
    }

    function remove(disconnectFunc, elem) {
        var self = this;
        disconnectFunc(self.id, elem.id).$promise.then(function(data) {
            toastr.success("Disconnected node");
            _.remove(self.list, elem);
        });
    }

    function add(connectFunc, item) {
        var self = this;
        if (_.any(self.list, { id: item.id }))
            return;

        connectFunc(self.id, item.id).$promise.then(function(data) {
            toastr.success("Connected node");
            self.list.push(data);
            self.new.title = "";
        });
    }

    function Node(id, connService, createFunc, searchFunc) {
        this.id = id;
        this.new = {
            title: ""
        };
        this.list = connService.query ? connService.query(this.id) : [];
        this.remove = connService.remove ? _.wrap(connService.remove, remove) : todo;
        this.add = connService.create ? _.wrap(connService.create, add) : todo;
        this.create = createFunc ? _.wrap(createFunc, create) : todo;
        this.search = searchFunc ? searchFunc : Search.query;
        _.bindAll(this);
    }

    function GoalNode(id, nodeService) {
        Node.call(this, id, nodeService ? nodeService.goals : {}, Goal.create, Search.queryGoals);
        this.info = DiscourseNode.goal;
        this.style = {
            templateUrl: "show_discourse_node_list.html",
            listCss: "discourse_goal_list",
        };
    }

    function ProblemNode(id, nodeService) {
        Node.call(this, id, nodeService ? nodeService.problems : {}, Problem.create, Search.queryProblems);
        this.info = DiscourseNode.problem;
        this.style = {
            templateUrl: "show_discourse_node_list.html",
            listCss: "discourse_problem_list",
        };
    }

    function IdeaNode(id, nodeService) {
        Node.call(this, id, nodeService ? nodeService.ideas : {}, Idea.create, Search.queryIdeas);
        this.info = DiscourseNode.idea;
        this.style = {
            templateUrl: "show_discourse_idea_list.html",
            listCss: "discourse_idea_list",
        };
    }
});
