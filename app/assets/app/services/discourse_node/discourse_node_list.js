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

    get.$inject = ["$injector", "$rootScope", "DiscourseNode", "EditService"];
    function get($injector, $rootScope, DiscourseNode, EditService) {
        function NodeInfoActions(nodeInfo) {
            return {
                writable: true,
                getLabel: () => nodeInfo.label,
                getCss: () => nodeInfo.css
            };
        }

        function NodeActions() {
            return {
                writable: false,
                getCss: node => DiscourseNode.get(node.label).css
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

            nested(nodeListCreate, servicePath, title) {
                this.isNested = true;
                this.nestedNodeLists.push({
                    servicePath,
                    create: service => nodeListCreate(service.$search(), title)
                });
            }
        }

        class WriteNodeModel extends NodeModel {
            constructor(nodeList, title, nodeInfo) {
                super(nodeList, title, `${nodeInfo.css}_list`);
                _.assign(this, NodeInfoActions(nodeInfo));
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
                    //TODO: workaround: this should definitely not be synched like this
                    let scratchpadNode = _.find(EditService.stack, n => elem.description === n.description && elem.title === elem.title && elem.addedTags === elem.addedTags && elem.id === elem.id);
                    scratchpadNode.id = data.id;
                    EditService.storeStack();
                });
            }
        }

        class ReadNodeModel extends NodeModel {
            constructor(nodeList, title) {
                super(nodeList, title, "read_node_list");
                _.assign(this, NodeActions());
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
        }

        return {
            write: _.mapValues(nodeListDefs, (v, k) => (nodeList, title = _.capitalize(v)) => {
                return new NodeList(new WriteNodeModel(nodeList, title, DiscourseNode[k]));
            }),
            read: (nodeList, title) => new NodeList(new ReadNodeModel(nodeList, title))
        };
    }
}
