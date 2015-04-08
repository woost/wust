angular.module("wust").service("DiscourseNodeList", function(DiscourseNode, Idea, Problem, Goal, Search) {
    [this.Goal, this.Problem, this.Idea] = nodeClasses();

    function create(func) {
        this.model.new.$save().$then(data => {
            humane.success("Created new node");
        });
    }

    function remove(func, elem) {
        elem.$destroy().$then(() => {
            humane.success("Disconnected node");
        });
    }

    function add(func, elem) {
        elem.$save().$then(data => {
            this.model.new.title = "";
            humane.success("Connected node");
        });
    }

    function nodeClasses() {
        class NodeList {
                constructor(service, connService = {}, searchService = Search.all) {
                this.model = {
                    new: service.$build({
                        title: ""
                    }),
                    list: connService.$search(),
                    remove: remove.bind(this),
                    add: add.bind(this),
                    create: create.bind(this),
                    search: searchService.$search
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
            constructor(nodeService = {}) {
                super(Goal, nodeService.goals, Search.goals);
                this.model.info = DiscourseNode.goal;
                this.model.style = {
                    templateUrl: "show_discourse_node_list.html",
                    listCss: "discourse_goal_list",
                };
            }
        }

        class ProblemNodeList extends NodeList {
            constructor(nodeService = {}) {
                super(Problem, nodeService.problems, Search.problems);
                this.model.info = DiscourseNode.problem;
                this.model.style = {
                    templateUrl: "show_discourse_node_list.html",
                    listCss: "discourse_problem_list",
                };
            }
        }

        class IdeaNodeList extends NodeList {
            constructor(nodeService = {}) {
                super(Idea, nodeService.ideas, Search.ideas);
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
