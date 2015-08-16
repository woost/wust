angular.module("wust.elements").provider("DiscourseNodeList", DiscourseNodeList);

DiscourseNodeList.$inject = [];

function DiscourseNodeList() {
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
            constructor(component, node, connectorType, writable) {
                this.component = component;
                this.node = node;
                this.connectorType = connectorType;
                this.writable = writable;
                this.isNested = false;
                this.nestedNodeLists = [];
                this.listId = _.uniqueId();

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

                let self = this;
                this.component.onCommit(changes => {
                    changes.newNodes.forEach(r => {
                        if (r.isHyperRelation) {
                            if (self.connectorType === SUCCESSORS && r.startId === self.node.id)
                                self.applyAllNested(r.endNode, r);
                            else if (self.connectorType === PREDECESSORS && r.endId === self.node.id)
                                self.applyAllNested(r.startNode, r);
                        }
                    });
                });
            }

            get list() {
                return this.node[this.nodeProperty];
            }

            exists(elem) {
                return _.any(this.list, {
                    id: elem.id
                });
            }

            applyAllNested(node, hyperNode) {
                if (node.nestedNodeLists !== undefined && node.nestedNodeLists[this.listId] !== undefined)
                    delete node.nestedNodeLists[this.listId];
                _.each(this.nestedNodeLists, def => this.applyNested(node, hyperNode, def));
            }

            applyNested(node, hyperNode, def) {
                node.nestedNodeLists = node.nestedNodeLists || {};
                if (node.nestedNodeLists[this.listId] === undefined) {
                    node.nestedNodeLists[this.listId] = [];
                }

                let nodeList = def(hyperNode);
                nodeList.model.setParent(this, node);
                node.nestedNodeLists[this.listId].push(nodeList);
            }

            getHyperRelationTo(node) {
                switch(this.connectorType) {
                    case PREDECESSORS:
                        return this.component.relationByIds(node.id, this.node.id);
                    case SUCCESSORS:
                        return this.component.relationByIds(this.node.id, node.id);
                }
            }

            nested(nodeListCreate, modelProperty) {
                this.isNested = true;
                let nestedNodeListDef = node => nodeListCreate(this.component, node, modelProperty);
                this.nestedNodeLists.push(nestedNodeListDef);
                _.each(this.list, node => {
                    this.applyNested(node, this.getHyperRelationTo(node), nestedNodeListDef);
                });
            }
        }

        class WriteNodeModel extends NodeModel {
            constructor(component, node, connectorType, modelProperty, nodeInfo) {
                super(component, node, connectorType, true);
                this.modelProperty = modelProperty;
                this.service = nodeInfo.service;
            }

            // the parentList is used in order to construct the correct url for
            // the api call. The reference node is needed as this.node is a
            // isHyperRelation for all nested lists, but the api refers to isHyperRelation
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
                    wrapped = this.service.$buildRaw(this.referenceNode.encode());
                } else {
                    let wrappedParent = this.parent.apiList;
                    wrapped = wrappedParent.$buildRaw(this.referenceNode.encode());
                }

                return wrapped[this.modelProperty];
            }

            remove(elem) {
                let self = this;
                this.apiList.$buildRaw(elem.encode()).$destroy().$then(() => {
                    humane.success("Disconnected node");
                    //TODO: response should include the deleted relation
                    let hyperNode = this.getHyperRelationTo(elem);
                    self.component.removeRelation(hyperNode.startId, hyperNode.endId);
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
                        _.each(data.graph.nodes, n => self.component.add(n));
                        self.component.commit();
                    });
                } else {
                    self.apiList.$buildRaw(elem).$save({}).$then(data => {
                        humane.success("Connected node");
                        _.each(data.graph.nodes, n => self.component.add(n));
                        self.component.commit();
                    });
                }
            }
        }

        class ReadNodeModel extends NodeModel {
            constructor(component, node, connectorType) {
                super(component, node, connectorType, false);
            }

            setParent() {}
        }

        class NodeList {
            constructor(nodeModel) {
                this.model = nodeModel;
            }

            nested(nodeListCreate, modelProperty) {
                this.model.nested(nodeListCreate, modelProperty);
                return this;
            }
        }

        return {
            write: _.mapValues(nodeListDefs, (v, k) => {
                return {
                    predecessors: (component, node, modelProperty) => {
                        return new NodeList(new WriteNodeModel(component, node, PREDECESSORS, modelProperty, DiscourseNode[k]));
                    },
                    successors: (component, node, modelProperty) => {
                        return new NodeList(new WriteNodeModel(component, node, SUCCESSORS, modelProperty, DiscourseNode[k]));
                    }
                };
            }),
            read: {
                predecessors: (component, node) => new NodeList(new ReadNodeModel(component, node, PREDECESSORS)),
                successors: (component, node) => new NodeList(new ReadNodeModel(component, node, SUCCESSORS))
            }
        };
    }
}

