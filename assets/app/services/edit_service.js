angular.module("wust.services").service("EditService", EditService);

EditService.$inject = ["Post", "Connectable", "Reference", "HistoryService", "store", "DiscourseNode", "ZenService"];

function EditService(Post, Connectable, Reference, HistoryService, store, DiscourseNode, ZenService) {
    let editStore = store.getNamespacedStore("edit");
    let self = this;

    class Session {
        constructor(other, lazyAdd = false, isConnects = false, newDiscussion = false) {
            // local id to identify nodes without an id
            this.localId = _.uniqueId();

            this.apply(other);

            this.expandedEditor = !!other.expandedEditor;

            this.service = isConnects ? Reference : Post;

            this.isConnects = isConnects;

            this.lazyAdd = lazyAdd;

            this.newDiscussion = newDiscussion;
        }

        apply({
            id, title, description, label, original, tags, localId, referenceNode, newDiscussion, isHyperRelation, classifications, tagClassifications, startId, endId, visible
        }) {
            tags = tags || [];
            classifications = classifications || [];
            tagClassifications = tagClassifications || [];
            this.id = id;
            this.visible = visible === undefined ? this.visible : !!visible;
            this.startId = startId;
            this.endId = endId;
            this.localId = localId === undefined ? this.localId : localId;
            this.title = title || "";
            this.description = description || "";
            this.label = label;
            this.referenceNode = referenceNode;
            this.newDiscussion = newDiscussion || false;
            this.isHyperRelation = isHyperRelation || false;
            this.tags = angular.copy(tags.map(t => t.$encode ? t.$encode() : t));
            this.classifications = angular.copy(classifications.map(t => t.$encode ? t.$encode() : t));
            this.tagClassifications = angular.copy(tagClassifications.map(t => t.$encode ? t.$encode() : t));
            this.original = original || {
                id: this.id,
                localId: this.localId,
                title: this.title,
                description: this.description,
                label: this.label,
                tags: angular.copy(tags.map(t => t.$encode ? t.$encode() : t)),
                classifications: angular.copy(classifications.map(t => t.$encode ? t.$encode() : t))
            };

            this.setValidityProperties();
        }

        setReference(reference) {
            this.newDiscussion = this.newDiscussion && !reference;
            this.referenceNode = reference && reference.encode ? reference.encode() : reference;
        }

        //TODO: we should rewrite the whole logic here, it is weird and hacky, but it works so i leave it as is :)
        dirtyModel(saveModel = false) {
            let dirtyModel;
            if (saveModel && this.id === undefined)
                dirtyModel = _.omit(_.pick(this, _.keys(this.original)), v => _.isEmpty(v));
            else
                dirtyModel = _.omit(_.pick(this, _.keys(this.original)), (v, k) => this.original[k] === v);

            delete dirtyModel.tags;
            let addedTags = _.reject(this.tags, t => t.id && _.any(this.original.tags, _.pick(t, "id"))).map(t => t.id ? _.pick(t, "id", "classifications") : _.pick(t, "title", "classifications"));
            let removedTags = _.reject(this.original.tags, t => !t.id || _.any(this.tags, _.pick(t, "id"))).map(t => t.id);

            addedTags.forEach(tag => {
                tag.classifications = this.tagClassifications.concat(tag.classifications);
            });

            if (addedTags.length > 0)
                dirtyModel.addedTags = addedTags;
            if (removedTags.length > 0)
                dirtyModel.removedTags = removedTags;

            if (("classifications" in dirtyModel) && dirtyModel.classifications.length === 0)
                delete dirtyModel.classifications;

            return dirtyModel;
        }

        save() {
            let dirtyModel = this.dirtyModel(true);
            this.setValidityProperties(dirtyModel);
            if (!this.canSave)
                return;

            // we do this in order to disable the form until we saved the node.
            this.unsetValidityProperties();

            let model = _.pick(this, "id");

            let referenceNode = this.referenceNode;
            let promise;
            if (referenceNode) {
                promise = connectNodes(this.encode(), referenceNode, dirtyModel);
            } else {
                promise = this.service.$buildRaw(model).$update(dirtyModel);
            }

            promise.$then(response => {
                let data = referenceNode ? response.node : response;

                if (this.isConnects) {
                    data.tags = this.tags;
                    data.startId = this.startId;
                    data.endId = this.endId;
                } else {
                    let appliedRequests = data.requestsTags && _.any(data.requestsTags, "applied") || data.requestsEdit && _.any(data.requestsEdit, "applied");
                    let hasRequests = !_.isEmpty(data.requestsTags) || !_.isEmpty(data.requestsEdit);
                    if (appliedRequests)
                        humane.success("Updated node");
                    else if (hasRequests)
                        humane.success("Created change request");
                    else
                        humane.success("Added new node");

                    let keeped;
                    if (data.requestsTags) {
                        let removed = data.requestsTags.filter(t => t.isRemove && t.applied).map(t => t.tag.id);
                        keeped = this.original.tags.filter(t => !_.contains(removed, t.id));
                    } else {
                        keeped = this.original.tags;
                    }

                    data.tags = _.uniq(data.tags.concat(keeped), "id");
                }

                if(referenceNode) {
                    let connects = _.find(response.graph.nodes, n => n.label === "CONNECTS" && n.startId === data.id && n.endId === referenceNode.id);
                    let session = editConnects(connects);
                    session.tags = this.classifications;
                    data.classifications = [];
                    session.save();
                }

                this.apply(data);

                if (referenceNode === undefined) {
                    HistoryService.updateCurrentView(this.encode());
                }

                if (!this.visible) {
                    _.remove(self.list, this);
                }

                storeEditList();

            }, response => {
                humane.error(response.$response.data);
                this.setValidityProperties();
            });

            return promise;
        }

        discard() {
            this.apply(this.original);
            this.onChange();
        }

        encode() {
            return this.id === undefined ? _.pick(this, _.keys(this.original)) : this.original;
        }

        onChange() {
            this.setValidityProperties();

            if (this.lazyAdd) {
                this.lazyAdd = false;
                editSession(this);
            }

            storeEditList();

            //TODO: workaround, it might be the case that we have the preview with this node. but if it is not the same instance as the edit post, we will not see any changes. so we'll check here and replace the zenservice node with our node.
            if (ZenService.visible && this.id !== undefined && ZenService.node.id === this.id && ZenService.node !== this) {
                ZenService.show(this);
            }
        }

        setValidityProperties(dirtyModel = this.dirtyModel()) {
            this.isPristine = _.isEmpty(dirtyModel);
            this.isLocal = this.id === undefined;
            //TODO: share validation code between scala and js
            this.isValid = this.isHyperRelation || (!_.isEmpty(this.title) && this.title.length <= 140);
            this.canSave = this.isValid && (this.isLocal || !this.isPristine);
        }

        unsetValidityProperties() {
            let dirtyModel = {};
            this.isPristine = true;
            this.canSave = false;
        }

        remove() {
            _.remove(self.list, this);
            storeEditList();
        }

        deleteNode() {
            if (this.isLocal)
                return;

            return this.service.$buildRaw(_.pick(this, "id")).$destroy().$then(() => {
                HistoryService.remove(this.id);
                this.remove();
                humane.success("Removed node");
            });
        }
    }

    this.list = restoreEditList();
    this.edit = edit;
    this.editAnswer = editAnswer;
    this.editNewDiscussion = editNewDiscussion;
    this.editConnects = editConnects;
    this.updateNode = updateNode;
    this.findNode = findNode;
    this.persist = storeEditList;
    this.forget = clearEditList;
    this.connectNodes = connectNodes;

    function restoreEditList() {
        //compact if something is really wrong and we have nulls in the localstorage. be forgiving.
        return _.map(_.compact(editStore.get("list", self.list) || []), s => new Session(s));
    }

    function clearEditList() {
        self.list = [];
        storeEditList();
    }

    function storeEditList() {
        editStore.set("list", self.list);
    }

    function updateNode(localId, node) {
        let existing = findNode(localId);
        if (existing !== undefined) {
            existing.apply(node);
            storeEditList();
        }
    }

    function findNode(localId) {
        if (localId === undefined)
            return undefined;

        return _.find(self.list, {
            localId
        });
    }

    function assureSessionExists(node, index) {
        let existingIdx = -1;
        if (node === undefined) {
            node = {};
            existingIdx = _.findIndex(self.list, elem => elem.isLocal && elem.isPristine);
        } else if (node.id !== undefined || node.localId !== undefined) {
            let searchFor = node.id === undefined ? "localId" : "id";
            existingIdx = _.findIndex(self.list, _.pick(node, searchFor));
        }

        let existing;
        let lazyadd = node.id !== undefined;
        if (existingIdx >= 0) {
            existing = self.list[existingIdx];
            if (!lazyadd)
                self.list.splice(existingIdx, 1);
        } else {
            // lazily add existing nodes, so they only appear in the scratchpad
            // if they were actually edited
            existing = new Session(node, lazyadd);
        }

        if (!lazyadd) {
            self.list.splice(index, 0, existing);
            storeEditList();
        }

        return existing;
    }

    function connectNodes(startNode, endNode, saveNode = startNode) {
        let localStart = startNode.id === undefined;
        let promise;
        if (endNode.isHyperRelation) {
            let start = Post.$buildRaw({
                id: endNode.startId
            });
            let hyper = start.connectsTo.$buildRaw({
                id: endNode.endId
            });
            promise = localStart ? hyper.connectsFrom.$create(saveNode) : hyper.connectsFrom.$buildRaw(_.pick(startNode, "id")).$save({});
        } else {
            let start = Connectable.$buildRaw(_.pick(endNode, "id"));
            promise = localStart ? start.connectsFrom.$create(saveNode) : start.connectsFrom.$buildRaw(_.pick(startNode, "id")).$save({});
        }

        promise.$then(response => {
            HistoryService.addConnectToCurrentView(endNode.id, response);
        });

        return promise;
    }

    function editConnects(node) {
        return new Session(node, false, true, false);
    }

    function editSession(session, index = 0) {
        self.list.splice(index, 0, session);
    }

    function editAnswer(node) {
        let existingAnswer = _.find(self.list, elem => elem.isLocal && elem.referenceNode && elem.referenceNode.id === node.id);
        if (existingAnswer === undefined) {
            let session = new Session({}, true, false, false);
            session.setReference(node);
            return session;
        } else {
            return edit(existingAnswer);
        }
    }

    function editNewDiscussion(tags = []) {
        let existingAnswer = _.find(self.list, elem => elem.isLocal && !elem.referenceNode && elem.newDiscussion && _.every(tags, tag => _.any(elem.tags, other => other.id === tag.id)));
        if (existingAnswer === undefined) {
            let session = new Session({}, true, false, true);
            session.tags = tags;
            return session;
        } else {
            // TODO: what about the tags of the new discussion
            return edit(existingAnswer);
        }
    }

    function edit(node, index = 0) {
        let session = assureSessionExists(node, index);
        //TODO: it might happen that we get a node without tags, for example it
        //happens when a node is dropped because encoding a node does not
        //return nested resources like tags.
        //in that case we load them
        if (node && node.id !== undefined && node.tags === undefined) {
            Post.$buildRaw(_.pick(node, "id")).tags.$search().$then(val => {
                let encoded = val.$encode();
                session.original.tags = encoded;
                //TODO: we should concat, it might actually be the case
                //that the user already added a new tag before we got the
                //result, but somehow this does not work?
                // session.tags.concat(encoded);
                session.tags = angular.copy(encoded);
                session.onChange();
            });
        }

        return session;
    }
}
