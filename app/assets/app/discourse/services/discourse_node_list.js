angular.module("wust.discourse").service("DiscourseNodeList", function(DiscourseNode, Idea, Problem, Goal, ProArgument, ConArgument, Search) {
    class NodeModel {
        constructor(service, connService, nodeInfo, title, nestable) {
            this.resetNew = () => {
                this.new = service.$build({
                    title: ""
                });
            };

            this.title = title;
            this.resetNew();
            this.waiting = [];
            this.info = nodeInfo;
            this.nestedNodeLists = [];
            this.nestable = nestable;
            this.list = connService.$search();

            if (nestable)
                this.list.$on("after-add", (node) => this.nestedNode(node));

            this.style = {
                templateUrl: "show_discourse_node_list.html",
                listCss: `${nodeInfo.css}_list`
            };

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

        addNode(elem) {
            if (this.exists(elem))
                return;

            this.list.$buildRaw(elem).$reveal();
        }

        removeNode(id) {
            let elem = _.find(this.list, {
                id: id
            });
            this.list.$remove(elem);
        }

        search(title) {
            return Search.$search({
                label: this.info.label,
                title: title
            });
        }

        isNested() {
            return this.nestable && _.any(this.nestedNodeLists);
        }

        nestedNode(node) {
            node.nestedNodeLists = _.map(this.nestedNodeLists, nodeList => nodeList.create(node[nodeList.servicePath]));
        }

        nested(nodeListConstructor, servicePath, title) {
            this.nestedNodeLists.push({
                servicePath,
                create: service => new nodeListConstructor(service, title)
            });
        }
    }

    class NodeList {
        constructor(service, connService, nodeInfo, title, nestable = true) {
            this.model = new NodeModel(service, connService, nodeInfo, title, nestable);
            console.log(this.model);
        }

        addNode(elem) {
            return this.model.addNode(elem);
        }

        removeNode(id) {
            return this.model.removeNode(id);
        }

        nested(nodeListConstructor, servicePath, title) {
            this.model.nested(nodeListConstructor, servicePath, title);
            return this;
        }
    }

    class GoalNodeList extends NodeList {
        constructor(connService, title = "Goals") {
            super(Goal, connService, DiscourseNode.Goal, title);
        }
    }

    class ProblemNodeList extends NodeList {
        constructor(connService, title = "Problems") {
            super(Problem, connService, DiscourseNode.Problem, title);
        }
    }

    class IdeaNodeList extends NodeList {
        constructor(connService, title = "Ideas") {
            super(Idea, connService, DiscourseNode.Idea, title);
        }
    }

    class ProArgumentList extends NodeList {
        constructor(connService, title = "Pros") {
            super(ProArgument, connService, DiscourseNode.ProArgument, title, false);
        }
    }

    class ConArgumentList extends NodeList {
        constructor(connService, title = "Cons") {
            super(ConArgument, connService, DiscourseNode.ConArgument, title, false);
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
