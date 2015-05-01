angular.module("wust.discourse").service("DiscourseNodeList", function(DiscourseNode, Idea, Problem, Goal, ProArgument, ConArgument, Search) {
    class NodeModel {
        constructor(service, connService, nodeInfo, title, styleInfo) {
            this.resetNew = () => {
                this.new = service.$build({
                    title: ""
                });
            };
            this.title = title;
            this.resetNew();
            this.list = connService.$search();
            this.waiting = [];
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
                this.resetNew();
            });
        }

        remove(elem) {
            elem.$destroy().$then(() => {
                humane.success("Disconnected node");
            });
        }

        exists(elem) {
            let search = {
                id: elem.id
            };

            return _.any(this.list, search) || _.any(this.waiting, search);
        }

        add(elem) {
            if (this.exists(elem))
                return;

            this.waiting.push(elem);
            this.list.$create(_.pick(elem, "id")).$then(data => {
                _.remove(this.waiting, elem);
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
        constructor(service, connService, nodeInfo, title, templateUrl) {
            this.model = new NodeModel(service, connService, nodeInfo, title, {
                templateUrl,
                listCss: `${nodeInfo.css}_list`
            });
        }

        addNode(elem) {
            if (this.model.exists(elem))
                return;

            this.model.list.$buildRaw(elem).$reveal();
        }

        removeNode(id) {
            let elem = _.find(this.model.list, {
                id: id
            });
            this.model.list.$remove(elem);
        }
    }

    class GoalNodeList extends NodeList {
        constructor(connService, title = "Goals") {
            super(Goal, connService, DiscourseNode.Goal, title, "show_discourse_node_list.html");
        }
    }

    class ProblemNodeList extends NodeList {
        constructor(connService, title = "Problems") {
            super(Problem, connService, DiscourseNode.Problem, title, "show_discourse_node_list.html");
        }
    }

    class IdeaNodeList extends NodeList {
        constructor(connService, title = "Ideas") {
            super(Idea, connService, DiscourseNode.Idea, title, "show_extended_discourse_node_list.html");
        }
    }

    class ProArgumentList extends NodeList {
        constructor(connService, title = "ProArguments") {
            super(ProArgument, connService, DiscourseNode.ProArgument, title, "show_discourse_node_list.html");
        }
    }

    class ConArgumentList extends NodeList {
        constructor(connService, title = "ProArguments") {
            super(ConArgument, connService, DiscourseNode.ProArgument, title, "show_discourse_node_list.html");
        }
    }

    return {
        Goal: GoalNodeList,
        Problem: ProblemNodeList,
        Idea: IdeaNodeList,
        ProArgument: ProArgumentList,
        ConArgument: ConArgumentList
    };
});
