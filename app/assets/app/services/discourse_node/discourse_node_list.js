angular.module("wust.services").provider("DiscourseNodeList", DiscourseNodeList);

DiscourseNodeList.$inject = [];

function DiscourseNodeList() {
    //TODO: move title out of discourse_node_list, should be individually set by the caller
    //TODO: input list or input service?
    //TODO: should handle event subscriptions?
    //TODO: take complete collection with nested nodes?
    let nodeListDefs = {};
    this.setList = setList;
    this.$get = get;

    function setList(modelName, modelPath) {
        nodeListDefs[modelName] = modelPath;
    }

    get.$inject = ["$injector", "$rootScope", "DiscourseNode"];
    function get($injector, $rootScope, DiscourseNode) {
        function NodeInfoActions(nodeInfo, mouseHandler) {
            return {
                writable: true,
                getLabel: () => nodeInfo.label,
                getCss: () => nodeInfo.css,
                onClick: mouseHandler.click || (node => nodeInfo.gotoState(node.id)),
                onHover: mouseHandler.hover || _.noop
            };
        }

        function NodeActions(mouseHandler) {
            return {
                writable: false,
                getCss: node => DiscourseNode.get(node.label).css,
                onClick: mouseHandler.click || (node => DiscourseNode.get(node.label).gotoState(node.id)),
                onHover: mouseHandler.hover || _.noop
            };
        }


        class NodeModel {
            constructor(nodeList, title, listCss) {
                this.list = nodeList;
                this.title = title;
                this.listCss = listCss;
                this.isNested = false;
                this.nestedNodeLists = [];

                //TODO: assure that the list is a restmod collection?
                // same for subscribe...
                if (this.list.$on !== undefined) {
                    // will create nested NodeLists for each added node
                    this.list.$on("after-add", (node) => {
                        node.nestedNodeLists = _.map(this.nestedNodeLists, list => list.create(node[list.servicePath]));
                    });
                } else {
                    console.warn("showing non-restmod collection");
                }
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
                if (this.list.$subscribeToLiveEvent === undefined) {
                    console.warn("Cannot subscribe non-schema collection");
                    return _.noop;
                }

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
                }, list.servicePath));

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

        class WriteNodeModel extends NodeModel {
            constructor(nodeList, title, mouseHandler, nodeInfo) {
                super(nodeList, title, `${nodeInfo.css}_list`);
                _.assign(this, NodeInfoActions(nodeInfo, mouseHandler));
            }

            remove(elem) {
                elem.$destroy().$then(() => {
                    humane.success("Disconnected node");
                });
            }

            add(elem) {
                if (this.exists(elem))
                    return;

                let payload = elem.id === undefined ? elem : _.pick(elem, "id");
                this.list.$create(payload).$then(data => {
                    humane.success("Connected node");
                });
            }
        }

        class ReadNodeModel extends NodeModel {
            constructor(nodeList, title, mouseHandler) {
                super(nodeList, title, "read_node_list");
                _.assign(this, NodeActions(mouseHandler));
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
            write: _.mapValues(nodeListDefs, (v, k) => (nodeList, title = _.capitalize(v), mouseHandler = {}) => {
                return new NodeList(new WriteNodeModel(nodeList, title, mouseHandler, DiscourseNode[k]));
            }),
            read: (nodeList, title, mouseHandler = {}) => new NodeList(new ReadNodeModel(nodeList, title, mouseHandler))
        };
    }
}
