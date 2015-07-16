angular.module("wust.services").provider("DiscourseNodeList", DiscourseNodeList);

DiscourseNodeList.$inject = [];

function DiscourseNodeList() {
    //TODO: move title out of discourse_node_list, should be individually set by the caller: but what about nesting?
    let nodeListDefs = {};
    this.setList = setList;
    this.$get = get;

    function setList(modelName, modelPath) {
        nodeListDefs[modelName] = modelPath;
    }

    get.$inject = ["$injector", "$rootScope", "DiscourseNode", "EditService"];

    function get($injector, $rootScope, DiscourseNode, EditService) {
        const PREDECESSORS = Symbol("predecessors");
        const SUCCESSORS = Symbol("successors");

        class NodeModel {
            constructor(component, node, connectorType, title, writable, listCss) {
                this.component = component;
                this.node = node;
                this.connectorType = connectorType;
                this.title = title;
                this.writable = writable;
                this.listCss = listCss;
                this.isNested = false;
                this.nestedNodeLists = [];

                switch(this.connectorType) {
                    case PREDECESSORS:
                        this.nodeProperty = "predecessors";
                        break;
                    case SUCCESSORS:
                        this.nodeProperty = "successors";
                        break;
                    default:
                        throw "Invalid connectorType for node list: " + this.connectorType;
                }

                this.component.onCommit(changes => _.each(changes.newNodes, n => this.applyAllNested(n)));
            }

            get list() {
                return this.node[this.nodeProperty];
            }

            exists(elem) {
                return _.any(this.list, {
                    id: elem.id
                });
            }

            applyAllNested(node) {
                _.each(this.nestedNodeLists, def => this.applyNested(node, def));
            }

            applyNested(node, def) {
                node.nestedNodeLists = node.nestedNodeLists || {};
                if (node.nestedNodeLists[this.connectorType] === undefined) {
                    node.nestedNodeLists[this.connectorType] = [];
                }

                //TODO: do not caclulate here, use graph wrapper
                let hyperNode;
                switch(this.connectorType) {
                    case PREDECESSORS:
                    hyperNode = _(this.node.inRelations).map("source").select("hyperEdge").find({
                        startId: node.id,
                        endId: this.node.id
                    });
                        break;
                    case SUCCESSORS:
                    hyperNode = _(this.node.outRelations).map("target").select("hyperEdge").find({
                        startId: this.node.id,
                        endId: node.id
                    });
                        break;
                }

                let nodeList = def(hyperNode);
                nodeList.model.setParent(this, node);
                node.nestedNodeLists[this.connectorType].push(nodeList);
            }

            nested(nodeListCreate, title, modelProperty) {
                this.isNested = true;
                let nestedNodeListDef = node => nodeListCreate(this.component, node, title, modelProperty);
                this.nestedNodeLists.push(nestedNodeListDef);
                _.each(this.list, node => this.applyNested(node, nestedNodeListDef));
            }
        }

        class WriteNodeModel extends NodeModel {
            constructor(component, node, connectorType, title, modelProperty, nodeInfo) {
                super(component, node, connectorType, title, true, `${nodeInfo.css}_list`);
                this.modelProperty = modelProperty;
                this.service = nodeInfo.service;
            }

            // the parentList is used in order to construct the correct url for
            // the api call. The reference node is needed as this.node is a
            // hyperEdge for all nested lists, but the api refers to hyperEdge
            // via their start and endnode.
            setParent(parentList, referenceNode) {
                // TODO: we actually could construct ReadNodeModels with a
                // NodeInfo, thus we would have a way to get the apilist of the
                // parent event if it was a ReadNodeModel
                if(!(parentList instanceof WriteNodeModel))
                    throw "Cannot nest WriteNodeModel into non-WriteNodeModels";

                this.parent = parentList;
                this._referenceNode = referenceNode;
            }

            get referenceNode() {
                return this._referenceNode || this.node;
            }

            get apiList() {
                let wrapped;
                if (this.parent === undefined) {
                    wrapped = this.service.$buildRaw(this.referenceNode.$encode());
                } else {
                    let wrappedParent = this.parent.apiList;
                    wrapped = wrappedParent.$buildRaw(this.referenceNode.$encode());
                }

                return wrapped[this.modelProperty];
            }

            remove(elem) {
                let self = this;
                // TODO: handle addition/removal on graph, not on apilist!
                this.apiList.$buildRaw(elem.$encode()).$destroy().$then(() => {
                    humane.success("Disconnected node");
                    //TODO: response should include the deleted relation
                    switch(this.connectorType) {
                        case PREDECESSORS:
                            _.each(elem.outRelations, r => r.hyperEdge ? self.component.removeNode(r) : self.component.removeRelation(r));
                            break;
                        case SUCCESSORS:
                            _.each(elem.inRelations, r => r.hyperEdge ? self.component.removeNode(r) : self.component.removeRelation(r));
                            break;
                    }

                    self.component.commit();
                });
            }

            add(elem) {
                if (this.exists(elem))
                    return;

                let self = this;
                if (elem.id === undefined) {
                    // TODO: element still has properties from edit_service Session
                    self.apiList.$buildRaw(_.pick(elem, "title", "description", "addedTags")).$save().$then(data => {
                        humane.success("Created and connected node");
                        EditService.updateNode(elem.localId, data.node);
                        //TODO: graph should only contain created items
                        //this.component.self.addNode(elem);
                        _.each(data.graph.nodes, n => self.component.addNode(n));
                        _.each(data.graph.edges, r => self.component.addRelation(r));
                        self.component.commit();
                        let node = self.component.nodes.byId(data.node.id);
                    });
                } else {
                    self.apiList.$buildRaw(elem).$save({}).$then(data => {
                        humane.success("Connected node");
                        _.each(data.graph.nodes, n => self.component.addNode(n));
                        _.each(data.graph.edges, r => self.component.addRelation(r));
                        self.component.commit();
                        let node = self.component.nodes.byId(data.node.id);
                    });
                }
            }
        }

        class ReadNodeModel extends NodeModel {
            constructor(component, node, connectorType, title) {
                super(component, node, connectorType, title, false, "read_node_list");
            }

            setParent() {}
        }

        class NodeList {
            constructor(nodeModel) {
                this.model = nodeModel;
            }

            nested(nodeListCreate, title, modelProperty) {
                this.model.nested(nodeListCreate, title, modelProperty);
                return this;
            }
        }

        return {
            write: _.mapValues(nodeListDefs, (v, k) => {
                return {
                    predecessors: (component, node, title, modelProperty) => {
                        return new NodeList(new WriteNodeModel(component, node, PREDECESSORS, title, modelProperty, DiscourseNode[k]));
                    },
                    successors: (component, node, title, modelProperty) => {
                        return new NodeList(new WriteNodeModel(component, node, SUCCESSORS, title, modelProperty, DiscourseNode[k]));
                    }
                };
            }),
            read: {
                predecessors: (component, node, title) => new NodeList(new ReadNodeModel(component, node, PREDECESSORS, title)),
                successors: (component, node, title) => new NodeList(new ReadNodeModel(component, node, SUCCESSORS, title))
            }
        };
    }
}

