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

    function Node(id, connService, createFunc, searchFunc) {
        this.id = id;
        this.new = {
            title: ""
        };
        this.list = connService.query ? connService.query(this.id) : [];
        this.remove = connService.remove ? remove(connService.remove).bind(this) : todo;
        this.add = connService.create ? add(connService.create).bind(this) : todo;
        this.create = createFunc ? create(createFunc).bind(this): todo;
        this.search = searchFunc ? searchFunc : Search.query;
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
