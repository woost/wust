angular.module("wust").service("DiscourseNodeList", function(DiscourseNode, Idea, Problem, Goal, Search) {
    class NodeModel {
        constructor(service, connService, nodeInfo, styleInfo) {
            this.new = service.$build({
                title: ""
            });
            this.list = connService.$search();
            this.info = nodeInfo;
            this.style = styleInfo;
            // this binds all this methods to this, which is needed here,
            // because all methods will be called as callbacks (not called
            // on the object)
            _.bindAll(this);
        }

        create() {
            this.new.$save().$then(data => {
                humane.success("Created new node");
                this.add(data);
                this.new.title = "";
            });
        }

        remove(elem) {
            elem.$destroy().$then(() => {
                humane.success("Disconnected node");
            });
        }

        exists(elem) {
            return _.any(this.list, {
                id: elem.id
            });
        }

        add(elem) {
            if (this.exists(elem))
                return;

            this.list.$create(_.pick(elem, "id")).$then(data => {
                humane.success("Connected node");
            });
        }

        search(title) {
            return Search.$search({
                label: this.info.label,
                title: title
            });
        }
    }

    class NodeList {
        constructor(service, connService, nodeInfo, styleInfo) {
            this.model = new NodeModel(service, connService, nodeInfo, styleInfo);
        }

        addNode(elem) {
            if (this.model.exists(elem))
                return;

            return this.model.list.push(elem);
        }

        removeNode(id) {
            return _.remove(this.model.list, {
                id: id
            });
        }
    }

    class GoalNodeList extends NodeList {
        constructor(nodeService = {}) {
            super(Goal, nodeService.goals, DiscourseNode.goal, {
                templateUrl: "show_discourse_node_list.html",
                listCss: "discourse_goal_list",
            });
        }
    }

    class ProblemNodeList extends NodeList {
        constructor(nodeService = {}) {
            super(Problem, nodeService.problems, DiscourseNode.problem, {
                templateUrl: "show_discourse_node_list.html",
                listCss: "discourse_problem_list",
            });
        }
    }

    class IdeaNodeList extends NodeList {
        constructor(nodeService = {}) {
            super(Idea, nodeService.ideas, DiscourseNode.idea, {
                templateUrl: "show_discourse_idea_list.html",
                listCss: "discourse_idea_list",
            });
        }
    }

    return {
        Goal: GoalNodeList,
        Problem: ProblemNodeList,
        Idea: IdeaNodeList
    };
});
