angular.module("wust").service("DiscourseNodeList", function(DiscourseNode, Idea, Problem, Goal, Search) {
    [this.Goal, this.Problem, this.Idea] = nodeClasses();

    function todo() {
        humane.error("not implemented");
    }

    function create(createFunc) {
        createFunc(this.model.new).$promise.then(data => {
            this.model.add(data);
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
            this.model.new.title = "";
            humane.success("Connected node");
        });
    }

    function nodeClasses() {
        class NodeList {
            constructor(id, createFunc, connService = {}, searchFunc = Search.query) {
                this.id = id;
                this.model = {
                    new: {
                        title: ""
                    },
                    list: connService.query ? connService.query(this.id) : [],
                    remove: connService.remove ? _.wrap(connService.remove, remove.bind(this)) : todo,
                    add: connService.create ? _.wrap(connService.create, add.bind(this)) : todo,
                    create: _.wrap(createFunc, create.bind(this)),
                    search: searchFunc
                };
            }

            addNode(elem) {
                if (_.any(this.model.list, {id: elem.id}))
                    return;

                return this.model.list.push(elem);
            }

            removeNode(id) {
                return _.remove(this.model.list, {id: id});
            }
        }

        class GoalNodeList extends NodeList {
            constructor(id, nodeService = {}) {
                super(id, Goal.create, nodeService.goals, Search.queryGoals);
                this.model.info = DiscourseNode.goal;
                this.model.style = {
                    templateUrl: "show_discourse_node_list.html",
                    listCss: "discourse_goal_list",
                };
            }
        }

        class ProblemNodeList extends NodeList {
            constructor(id, nodeService = {}) {
                super(id, Problem.create, nodeService.problems, Search.queryProblems);
                this.model.info = DiscourseNode.problem;
                this.model.style = {
                    templateUrl: "show_discourse_node_list.html",
                    listCss: "discourse_problem_list",
                };
            }
        }

        class IdeaNodeList extends NodeList {
            constructor(id, nodeService = {}) {
                super(id, Idea.create, nodeService.ideas, Search.queryIdeas);
                this.model.info = DiscourseNode.idea;
                this.model.style = {
                    templateUrl: "show_discourse_idea_list.html",
                    listCss: "discourse_idea_list",
                };
            }
        }

        return [GoalNodeList, ProblemNodeList, IdeaNodeList];
    }
});
