angular.module("wust.elements").provider("DiscourseNodeList", DiscourseNodeList);

DiscourseNodeList.$inject = [];

function DiscourseNodeList() {
    let nodeListDefs = {};
    this.setList = setList;
    this.$get = get;

    function setList(modelName, modelPath) {
        nodeListDefs[modelName] = modelPath;
    }

    get.$inject = ["$injector", "DiscourseNode", "EditService"];

    function get($injector, DiscourseNode, EditService) {
        const PREDECESSORS = 0;
        const SUCCESSORS = 1;
        const PARALLELS = 2;

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
                    case PARALLELS:
                        this.nodeProperty = "parallels";
                        break;
                    default:
                        throw "Invalid connectorType for node list: " + this.connectorType;
                }

                let self = this;
                this.deregisterCommit = this.component.onCommit(changes => {
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

            deregister() {
                this.deregisterCommit();
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
                if (this.connectorType === PARALLELS)
                    throw "Cannot nest parallel discourse nodes";

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
                if (connectorType === PARALLELS)
                    throw "Cannot create WriteNodeModel of parallel discourse nodes";

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
                    // humane.success("Disconnected node");
                    //TODO: response should include the deleted relation
                    let hyperNode = this.getHyperRelationTo(elem);
                    self.component.removeRelation(hyperNode.startId, hyperNode.endId);
                    self.component.commit();
                }, response => humane.error(response.$response.data));
            }

            canAdd(elem) {
                return this.writable && !this.component.rootNode.isDeleted && !this.exists(elem) && elem.id !== this.referenceNode.id;
            }

            add(elem) {
                if (!this.canAdd(elem))
                    return;

                let self = this;

                // first we check whether the node is currenlty edited and
                // has unsaved changes or is fresh.
                let editedNode = EditService.findNode(elem.localId);
                if (editedNode !== undefined && editedNode.canSave) {
                    editedNode.save().$then(connectNode);
                } else {
                    connectNode(elem);
                }

                function connectNode(node) {
                    if (node.id === undefined) {
                        console.warn("Tried to connect local node which is not in the EditService", node);
                    } else {
                        self.apiList.$buildRaw(node).$save({}).$then(data => {
                            // humane.success("Connected node");
                            addToComponent(data);
                        }, response => humane.error(response.$response.data));
                    }
                }

                // TODO: I am here because nobody implemented tags in write responses
                function addToComponent(response) {
                    let newElem = response.node;
                    newElem = _.find(response.graph.nodes, {
                        id: newElem.id
                    });

                    newElem.tags = elem.tags;

                    _.each(response.graph.nodes, n => self.component.add(n));
                    self.component.commit();
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
                successors: (component, node) => new NodeList(new ReadNodeModel(component, node, SUCCESSORS)),
                parallels: (component, node) => new NodeList(new ReadNodeModel(component, node, PARALLELS)),
            }
        };
    }
}

