angular.module("wust.discourse").provider("DiscourseNodeList", DiscourseNodeList);

DiscourseNodeList.$inject = [];

function DiscourseNodeList() {
    let nodeListDefs = {};
    this.setList = setList;
    this.$get = get;

    function setList(modelName, modelPath) {
        nodeListDefs[modelName] = modelPath;
    }

    get.$inject = ["$injector", "$rootScope", "DiscourseNode", "Search"];
    function get($injector, $rootScope, DiscourseNode, Search) {
        class NodeModel {
            constructor(connService, title, listCss, templateUrl) {
                this.style = {
                    listCss,
                    templateUrl
                };

                this.isNested = false;
                this.title = title;
                this.nestedNodeLists = [];
                this.list = connService.$search();
                this.waiting = [];

                // will create nested NodeLists for each added node
                this.list.$on("after-add", (node) => {
                    node.nestedNodeLists = _.map(this.nestedNodeLists, nodeList => nodeList.create(node[nodeList.servicePath]));
                });
            }

            exists(elem) {
                let search = {
                    id: elem.id
                };

                return _.any(this.list, search) || _.any(this.waiting, search);
            }

            nested(nodeListCreate, servicePath, title) {
                this.isNested = true;
                this.nestedNodeLists.push({
                    servicePath,
                    create: service => nodeListCreate(service, title)
                });
            }

            subscribe(nodeList) {
                let onConnectionChange = (list, message) => $rootScope.$apply(() => {
                    switch (message.type) {
                        case "connect":
                            list.addNode(message.data);
                            break;
                        case "disconnect":
                            list.removeNode(message.data.id);
                            break;
                        default:
                    }
                });

                let unsubscribeFuncs = _.map(this.nestedNodeLists, (list, i) => this.list.$subscribeToLiveEvent(m => {
                    let node = _.find(this.list, {id: m.reference});
                    if (node !== undefined) {
                        let list = node.nestedNodeLists[i];
                        onConnectionChange(list, m);
                    }
                }, `/${list.servicePath}`));

                unsubscribeFuncs.push(this.list.$subscribeToLiveEvent(m => onConnectionChange(nodeList, m)));

                return () => _.each(unsubscribeFuncs, func => func());
            }
        }

        class ReadNodeModel extends NodeModel {
            constructor(connService, title, listCss) {
                super(connService, title, listCss, "read_discourse_node_list.html");
            }
        }

        class TypedReadNodeModel extends ReadNodeModel {
            constructor(connService, nodeInfo, title) {
                super(connService, title);
                this.info = nodeInfo;
            }

            getCss() {
                return this.info.css;
            }

            getState(node) {
                return this.info.getState(node.id);
            }
        }

        class AnyReadNodeModel extends ReadNodeModel {
            constructor(connService, title) {
                super(connService, title);
            }

            getCss(node) {
                return DiscourseNode.get(node.label).css;
            }

            getState(node) {
                return DiscourseNode.get(node.label).getState(node.id);
            }
        }

        class WriteNodeModel extends NodeModel {
            constructor(service, connService, nodeInfo, title) {
                super(connService, title, `${nodeInfo.css}_list`, "write_discourse_node_list.html");

                this.info = nodeInfo;
                this.resetNew = () => {
                    this.new = service.$build({
                        title: ""
                    });
                };

                this.resetNew();
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
        }

        class NodeList {
            constructor(nodeModel) {
                this.model = nodeModel;
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

            nested(nodeListCreate, servicePath, title) {
                this.model.nested(nodeListCreate, servicePath, title);
                return this;
            }

            subscribe() {
                return this.model.subscribe(this);
            }
        }

        return {
            write: _.mapValues(nodeListDefs, (v, k) => (connService, title = _.capitalize(v)) => {
                return new NodeList(new WriteNodeModel($injector.get(k), connService, DiscourseNode[k], title));
            }),
            read: _.mapValues(nodeListDefs, (v, k) => (connService, title = _.capitalize(v)) => {
                return new NodeList(new TypedReadNodeModel(connService, DiscourseNode[k], title));
            }),
            Any: (connService, title) => new NodeList(new AnyReadNodeModel(connService, title))
        };
    }
}
