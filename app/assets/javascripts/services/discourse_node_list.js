angular.module("wust").service("DiscourseNodeList", function(DiscourseNode, Idea, Problem, Goal, Search) {
    [this.Goal, this.Problem, this.Idea] = nodeClasses();

    function todo() {
        toastr.error("not implemented");
    }

    function create(createFunc) {
        createFunc(this.new).$promise.then(data => {
            toastr.success("Created new node");
            this.add(data);
        });
    }

    function remove(disconnectFunc, elem) {
        disconnectFunc(this.id, elem.id).$promise.then(() => {
            toastr.success("Disconnected node");
            _.remove(this.list, elem);
        });
    }

    function add(connectFunc, item) {
        connectFunc(this.id, item.id).$promise.then(data => {
            toastr.success("Connected node");
            this.list.push(data);
            this.new.title = "";
        });
    }

    function withUniq(func, item) {
        if (_.any(this.list, {
            id: item.id
        }))
            return;

        func(item);
    }

    function nodeClasses() {
        class Node {
            constructor(id, createFunc, connService = {}, searchFunc = Search.query) {
                this.id = id;
                this.new = {
                    title: ""
                };

                this.list = connService.query ? connService.query(this.id) : [];
                this.remove = connService.remove ? _.wrap(connService.remove, remove) : todo;
                this.add = connService.create ? _.wrap(_.wrap(connService.create, add).bind(this), withUniq) : todo;
                this.create = _.wrap(createFunc, create);
                this.search = searchFunc;
                _.bindAll(this);

                this.push = _.wrap(this.list.push.bind(this.list), withUniq);
            }
        }

        class GoalNode extends Node {
            constructor(id, nodeService = {}) {
                super(id, Goal.create, nodeService.goals, Search.queryGoals);
                this.info = DiscourseNode.goal;
                this.style = {
                    templateUrl: "show_discourse_node_list.html",
                    listCss: "discourse_goal_list",
                };
            }
        }

        class ProblemNode extends Node {
            constructor(id, nodeService = {}) {
                super(id, Problem.create, nodeService.problems, Search.queryProblems);
                this.info = DiscourseNode.problem;
                this.style = {
                    templateUrl: "show_discourse_node_list.html",
                    listCss: "discourse_problem_list",
                };
            }
        }

        class IdeaNode extends Node {
            constructor(id, nodeService = {}) {
                super(id, Idea.create, nodeService.ideas, Search.queryIdeas);
                this.info = DiscourseNode.idea;
                this.style = {
                    templateUrl: "show_discourse_idea_list.html",
                    listCss: "discourse_idea_list",
                };
            }
        }

        return [GoalNode, ProblemNode, IdeaNode];
    }
});
