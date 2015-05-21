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
            constructor(nodeList, title, listCss, templateUrl) {
                this.style = {
                    listCss,
                    templateUrl
                };

                this.isNested = false;
                this.title = title;
                this.nestedNodeLists = [];
                this.list = nodeList;

                // will create nested NodeLists for each added node
                this.list.$on("after-add", (node) => {
                    node.nestedNodeLists = _.map(this.nestedNodeLists, list => list.create(node[list.servicePath]));
                });
            }

            exists(elem) {
                return _.any(this.list, {
                    id: elem.id
                });
            }

            removeLocally(id) {
                let elem = _.find(this.list, {
                    id: id
                });

                this.list.$remove(elem);
            }

            addLocally(elem) {
                if (this.exists(elem))
                    return;

                this.list.$buildRaw(elem).$reveal();
            }

            nested(nodeListCreate, servicePath, title) {
                this.isNested = true;
                this.nestedNodeLists.push({
                    servicePath,
                    create: service => nodeListCreate(service.$search(), title)
                });
            }

            subscribe(unsubscribe) {
                let onConnectionChange = (list, message) => $rootScope.$apply(() => {
                    switch (message.type) {
                        case "connect":
                            list.addLocally(message.data);
                            break;
                        case "disconnect":
                            list.removeLocally(message.data.id);
                            break;
                        default:
                    }
                });

                let unsubscribeFuncs = _.map(this.nestedNodeLists, (list, i) => this.list.$subscribeToLiveEvent(m => {
                    let node = _.find(this.list, {id: m.reference});
                    if (node !== undefined) {
                        let nestedList = node.nestedNodeLists[i];
                        onConnectionChange(nestedList.model, m);
                    }
                }, `/${list.servicePath}`));

                unsubscribeFuncs.push(this.list.$subscribeToLiveEvent(m => onConnectionChange(this, m)));

                let unsubscribeFunc = () => _.each(unsubscribeFuncs, func => func());
                if (unsubscribe) {
                    let deregisterEvent = $rootScope.$on("$stateChangeSuccess", () => {
                        unsubscribeFunc();
                        deregisterEvent();
                    });
                }

                return unsubscribeFunc;
            }
        }

        class ReadNodeModel extends NodeModel {
            constructor(nodeList, title, listCss) {
                super(nodeList, title, listCss, "read_discourse_node_list.html");
            }
        }

        class TypedReadNodeModel extends ReadNodeModel {
            constructor(nodeList, nodeInfo, title) {
                super(nodeList, title, `${nodeInfo.css}_read_list`);
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
            constructor(nodeList, title) {
                super(nodeList, title, "any_read_list");
            }

            getCss(node) {
                return DiscourseNode.get(node.label).css;
            }

            getState(node) {
                return DiscourseNode.get(node.label).getState(node.id);
            }
        }

        class WriteNodeModel extends NodeModel {
            constructor(service, nodeList, nodeInfo, title) {
                super(nodeList, title, `${nodeInfo.css}_list`, "write_discourse_node_list.html");

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

                this.list.$create(_.pick(elem, "id")).$reveal().$then(data => {
                    humane.success("Connected node");
                }).$promise.catch(() => this.removeLocally(elem.id));
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

            nested(nodeListCreate, servicePath, title) {
                this.model.nested(nodeListCreate, servicePath, title);
                return this;
            }

            subscribe(unsubscribe = true) {
                return this.model.subscribe(unsubscribe);
            }
        }

        return {
            write: _.mapValues(nodeListDefs, (v, k) => (nodeList, title = _.capitalize(v)) => {
                return new NodeList(new WriteNodeModel($injector.get(k), nodeList, DiscourseNode[k], title));
            }),
            read: _.mapValues(nodeListDefs, (v, k) => (nodeList, title = _.capitalize(v)) => {
                return new NodeList(new TypedReadNodeModel(nodeList, DiscourseNode[k], title));
            }),
            Any: (nodeList, title) => new NodeList(new AnyReadNodeModel(nodeList, title))
        };
    }
}
