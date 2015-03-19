angular.module("wust").service("DiscourseNodeList", function(DiscourseNode, Idea, Problem, Goal, Search) {
    [this.Goal, this.Problem, this.Idea] = nodeClasses();

    function todo() {
        humane.error("not implemented");
    }

    function create(createFunc) {
        createFunc(this.new).$promise.then(data => {
            this.add(data);
            humane.success("Created new node");
        });
    }

    function remove(disconnectFunc, elem) {
        disconnectFunc(this.id, elem.id).$promise.then(() => {
            this.removeNode(elem.id);
            humane.success("Disconnected node");
        });
    }

    function add(connectFunc, item) {
        connectFunc(this.id, item.id).$promise.then(data => {
            this.addNode(data);
            this.new.title = "";
            humane.success("Connected node");
        });
    }

    function nodeClasses() {
        class NodeList {
            constructor(id, createFunc, connService = {}, searchFunc = Search.query) {
                this.id = id;
                this.new = {
                    title: ""
                };

                this.list = connService.query ? connService.query(this.id) : [];
                this.remove = connService.remove ? _.wrap(connService.remove, remove) : todo;
                this.add = connService.create ? _.wrap(connService.create, add) : todo;
                this.create = _.wrap(createFunc, create);
                this.search = searchFunc;
                _.bindAll(this);
            }

            addNode(elem) {
                if (_.any(this.list, {id: elem.id}))
                    return;

                this.list.push(elem);
            }

            removeNode(id) {
                _.remove(this.list, {id: id});
            }
        }

        class GoalNodeList extends NodeList {
            constructor(id, nodeService = {}) {
                super(id, Goal.create, nodeService.goals, Search.queryGoals);
                this.info = DiscourseNode.goal;
                this.style = {
                    templateUrl: "show_discourse_node_list.html",
                    listCss: "discourse_goal_list",
                };
            }
        }

        class ProblemNodeList extends NodeList {
            constructor(id, nodeService = {}) {
                super(id, Problem.create, nodeService.problems, Search.queryProblems);
                this.info = DiscourseNode.problem;
                this.style = {
                    templateUrl: "show_discourse_node_list.html",
                    listCss: "discourse_problem_list",
                };
            }
        }

        class IdeaNodeList extends NodeList {
            constructor(id, nodeService = {}) {
                super(id, Idea.create, nodeService.ideas, Search.queryIdeas);
                this.info = DiscourseNode.idea;
                this.style = {
                    templateUrl: "show_discourse_idea_list.html",
                    listCss: "discourse_idea_list",
                };
            }
        }

        return [GoalNodeList, ProblemNodeList, IdeaNodeList];
    }
});
