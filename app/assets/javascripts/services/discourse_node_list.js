angular.module("wust").service('DiscourseNodeList', function(DiscourseNode, Idea, Problem, Goal, Search) {
    [ this.Goal, this.Problem, this.Idea ] = nodeClasses();

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
        disconnectFunc(this.id, elem.id).$promise.then(data => {
            toastr.success("Disconnected node");
            _.remove(this.list, elem);
        });
    }

    function add(connectFunc, item) {
        if (_.any(this.list, {
            id: item.id
        }))
            return;

        connectFunc(this.id, item.id).$promise.then(data => {
            toastr.success("Connected node");
            this.list.push(data);
            this.new.title = "";
        });
    }

    function nodeClasses() {
        class Node {
            constructor(id, connService, createFunc, searchFunc) {
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
        }

        class GoalNode extends Node {
            constructor(id, nodeService) {
                super(id, nodeService ? nodeService.goals : {}, Goal.create, Search.queryGoals);
                this.info = DiscourseNode.goal;
                this.style = {
                    templateUrl: "show_discourse_node_list.html",
                    listCss: "discourse_goal_list",
                };
            }
        }

        class ProblemNode extends Node {
            constructor(id, nodeService) {
                super(id, nodeService ? nodeService.problems : {}, Problem.create, Search.queryProblems);
                this.info = DiscourseNode.problem;
                this.style = {
                    templateUrl: "show_discourse_node_list.html",
                    listCss: "discourse_problem_list",
                };
            }
        }

        class IdeaNode extends Node {
            constructor(id, nodeService) {
                super(id, nodeService ? nodeService.ideas : {}, Idea.create, Search.queryIdeas);
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
