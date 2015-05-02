angular.module("wust.discourse").provider("DiscourseNodeList", function() {
    let nodeListDefs = {};
    this.setList = setList;
    this.$get = get;

    function setList(modelName, modelPath) {
        nodeListDefs[modelName] = modelPath;
    }

    function get($injector, DiscourseNode, Search) {
        class NodeModel {
            constructor(service, connService, nodeInfo, title) {
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
                this.list = connService.$search();

                // will create nested NodeLists for each added node
                this.list.$on("after-add", (node) => this.nestedNode(node));

                // defines the styling information for the DiscourseNodeList
                // directive
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

            exists(elem) {
                let search = {
                    id: elem.id
                };

                return _.any(this.list, search) || _.any(this.waiting, search);
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

            isNested() {
                return _.any(this.nestedNodeLists);
            }

            nestedNode(node) {
                node.nestedNodeLists = _.map(this.nestedNodeLists, nodeList => nodeList.create(node[nodeList.servicePath]));
            }

            nested(nodeListCreate, servicePath, title) {
                this.nestedNodeLists.push({
                    servicePath,
                    create: service => nodeListCreate(service, title)
                });
            }
        }

        class NodeList {
            constructor(service, connService, nodeInfo, title) {
                this.model = new NodeModel(service, connService, nodeInfo, title);
            }

            addNode(elem) {
                return this.model.addNode(elem);
            }

            removeNode(id) {
                return this.model.removeNode(id);
            }

            nested(nodeListCreate, servicePath, title) {
                this.model.nested(nodeListCreate, servicePath, title);
                return this;
            }
        }

        return _.mapValues(nodeListDefs, (v, k) => (connService, title = _.capitalize(v)) => {
            return new NodeList($injector.get(k), connService, DiscourseNode[k], title);
        });
    }
});
