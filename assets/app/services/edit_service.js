angular.module("wust.services").service("EditService", EditService);

EditService.$inject = ["Session", "Post", "Connectable", "Connects", "HistoryService", "store", "DiscourseNode", "ZenService", "Auth"];

function EditService(Session, Post, Connectable, Connects, HistoryService, store, DiscourseNode, ZenService, Auth) {
    let editStore = store.getNamespacedStore(`edit.${Auth.current.userId}`);
    let self = this;

    class EditSession {
        constructor(other = {}, isOriginal = false) {

            // local id to identify nodes without an id
            this.localId = _.uniqueId();
            this.lazyAdd = !!other.lazyAdd;
            this.isConnects = !!other.isConnects;
            this.newDiscussion = !!other.newDiscussion;

            this.expandedEditor = !!other.expandedEditor;
            this.visible = !!other.visible;
            this.isHyperRelation = !!other.isHyperRelation; //TODO: why am i here?
            this.startId = other.startId;
            this.endId = other.endId;
            this.triedSave = !!other.triedSave;

            this.referenceNode = other.referenceNode;
            this.classifications = angular.copy((other.classifications || []).map(t => t.$encode ? t.$encode() : t));
            this.tagClassifications = angular.copy((other.tagClassifications || []).map(t => t.$encode ? t.$encode() : t));

            this.apply(other, isOriginal, other.original);
        }

        get service() {
            return this.isConnects ? Connects : Post;
        }

        unsetOneTimeProperties() {
            this.newDiscussion = false;
            this.referenceNode = undefined;
            this.classifications = [];
            this.tagClassifications = [];
        }

        apply({
            id, title, description, tags
        }, isOriginal = false, defaultOriginal = {}) {
            this.id = id === undefined ? this.id : id;
            this.title = title || "";
            this.description = description || "";
            this.tags = angular.copy((tags || []).map(t => t.$encode ? t.$encode() : t));
            this.tags.forEach(t => t.classifications = t.classifications || []);
            this.original = isOriginal ? {
                title: this.title,
                description: this.description,
                tags: angular.copy(this.tags)
            } : {
                title: defaultOriginal.title || "",
                description: defaultOriginal.description || "",
                tags: defaultOriginal.tags || []
            };

            this.setValidityProperties();
        }

        setReference(reference) {
            this.newDiscussion = this.newDiscussion && !reference;
            this.referenceNode = reference && reference.encode ? reference.encode() : reference;
        }

        //TODO: we should rewrite the whole logic here, it is weird and hacky, but it works so i leave it as is :)
        dirtyModel() {
            let dirtyModel = _.omit(_.pick(this, _.keys(this.original)), (v, k) => this.original[k] === v);

            delete dirtyModel.tags;
            let addedTags = _.reject(this.tags, t => t.id && _.any(this.original.tags, orig => orig.id === t.id && orig.classifications.map(c => c.id) === t.classifications.map(c => c.id))).map(t => t.id ? _.pick(t, "id", "classifications") : _.pick(t, "title", "classifications"));
            let removedTags = _.reject(this.original.tags, t => !t.id || _.any(this.tags, _.pick(t, "id"))).map(t => t.id);

            addedTags.forEach(tag => {
                // TODO: why do we have nulls in classifications?
                tag.classifications = _.compact(this.tagClassifications.concat(tag.classifications)).map(t => _.pick(t, "id"));
            });

            if (addedTags.length > 0)
                dirtyModel.addedTags = addedTags;
            if (removedTags.length > 0)
                dirtyModel.removedTags = removedTags;

            return dirtyModel;
        }

        save() {
            let dirtyModel = this.dirtyModel();
            this.setValidityProperties(dirtyModel);
            this.triedSave = true;
            if (!this.canSave)
                return false;

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
                this.triedSave = false;
                let data = referenceNode ? response.node : response;

                if (this.isConnects) {
                    data.tags = this.tags;
                } else {
                    let appliedRequests = data.requestsTags && _.any(data.requestsTags, "status") || data.requestsEdit && _.any(data.requestsEdit, "status");
                    let hasRequests = !_.isEmpty(data.requestsTags) || !_.isEmpty(data.requestsEdit);
                    if (appliedRequests)
                        humane.success("Updated node");
                    else if (hasRequests)
                        humane.success("Created change request");
                    else
                        humane.success("Added new node");

                    let keeped;
                    if (data.requestsTags) {
                        let removed = data.requestsTags.filter(t => t.isRemove && t.status).map(t => t.tag.id);
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
                    session.save();
                }

                if (this.visible && this.isLocal) {
                    Session.marks.add(data.id);
                }

                this.apply(data, true);

                if (referenceNode === undefined) {
                    HistoryService.updateCurrentView(this.encode());
                }

                if (!this.visible) {
                    _.remove(self.list, this);
                }

                this.unsetOneTimeProperties();
                storeEditList();

            }, response => {
                humane.error(response.$response.data);
                this.setValidityProperties();
            });

            return promise;
        }

        discard() {
            this.apply(this.original, true);
            this.onChange();
        }

        encode() {
            let encoded = _.pick(this, _.keys(this.original));
            encoded.id = this.id;
            return encoded;
        }

        onChange() {
            this.setValidityProperties();

            if (this.lazyAdd) {
                this.lazyAdd = false;
                self.list.splice(0, 0, this);
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
            let defaultValidity = ValidityEntry("Invalid", true);
            this.validity = {
                title: defaultValidity,
                description: defaultValidity,
                tags: defaultValidity
            };

            if (this.isConnects) {
            } else {
                this.validity.title = ValidityEntry("Title may not be empty", !_.isEmpty(this.title))
                                        .and(ValidityEntry("Title must be less than 140 characters", this.title.length <= 140));

                if (this.newDiscussion) {
                    this.validity.tags = ValidityEntry("New discussion needs context", !_.isEmpty(this.tags));
                }
            }

            this.isValid = _.every(this.validity, { valid: true });
            this.canSave = this.isValid && (this.isLocal || !this.isPristine);

            function ValidityEntry(errorMsg, valid) {
                let self = {};
                self.valid = valid;
                self.message = valid ? "Success" : errorMsg;
                self.and = other => self.valid ? other : self;
                return self;
            }
        }

        unsetValidityProperties() {
            let dirtyModel = {};
            this.isPristine = true;
            this.canSave = false;
        }

        remove() {
            _.remove(self.list, this);
            if (this.visible && !this.isLocal) {
                Session.marks.destroy(this.id);
            }

            storeEditList();
        }

        deleteNode() {
            if (this.isLocal)
                return;

            return this.service.$buildRaw(_.pick(this, "id")).$destroy().$then(data => {
                if (data.$response.status === 204) { //NoContent response => instant delete
                    HistoryService.remove(this.id);
                    this.remove();
                }
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
    this.scratchpad = {
        showEdits: false
    };

    function restoreEditList() {
        //compact if something is really wrong and we have nulls in the localstorage. be forgiving.
        return _.map(_.compact(editStore.get("list") || []), s => new EditSession(s));
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
            existing.apply(node, true);
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

    function assureSessionExists(node, visible, index) {
        let existingIdx = -1;
        if (node === undefined) {
            node = {};
            existingIdx = _.findIndex(self.list, elem => elem.isLocal && elem.isPristine);
        } else if (node.id !== undefined || node.localId !== undefined) {
            let searchFor = node.id === undefined ? "localId" : "id";
            existingIdx = _.findIndex(self.list, _.pick(node, searchFor));
        }

        let existing;
        let hasId = node.id !== undefined;
        if (existingIdx >= 0) {
            existing = self.list[existingIdx];
            if (index !== undefined) {
                self.list.splice(existingIdx, 1);
                self.list.splice(index, 0, existing);
            }
        } else {
            // lazily add existing nodes, so they only appear in the scratchpad
            // if they were actually edited
            existing = new EditSession(node, hasId);
            if (visible && hasId) {
                Session.marks.add(node.id);
            }
            if (!visible && hasId) {
                existing.lazyAdd = true;
            } else {
                self.list.splice(index === undefined ? 0 : index, 0, existing);
            }
        }

        if (hasId) {
            if (existing.isPristine) {
                existing.apply(node, true);
            } else {
                existing.apply(existing, false, node);
            }
        }

        if (visible) {
            existing.visible = true;
        }

        storeEditList();

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
        let session = new EditSession(node);
        session.isConnects = true;
        return session;
    }

    function editAnswer(node) {
        let existingAnswer = _.find(self.list, elem => elem.isLocal && elem.referenceNode && elem.referenceNode.id === node.id);
        if (existingAnswer === undefined) {
            let session = new EditSession();
            session.lazyAdd = true;
            session.setReference(node);
            return session;
        } else {
            return edit(existingAnswer);
        }
    }

    function editNewDiscussion(tags = []) {
        let existingAnswer = _.find(self.list, elem => elem.isLocal && !elem.referenceNode && elem.newDiscussion && _.every(tags, tag => _.any(elem.tags, other => other.id === tag.id)));
        if (existingAnswer === undefined) {
            let session = new EditSession({tags});
            session.lazyAdd = true;
            session.newDiscussion = true;
            return session;
        } else {
            // TODO: what about the tags of the new discussion
            return edit(existingAnswer);
        }
    }

    function edit(node, visible, index) {
        let session = assureSessionExists(node, !!visible, index);
        //TODO: it might happen that we get a node without tags, for example it
        //happens when a node is dropped because encoding a node does not
        //return nested resources like tags.
        //in that case we load them
        if (node && node.id !== undefined && node.tags === undefined) {
            Post.$buildRaw(_.pick(node, "id")).tags.$search().$then(val => {
                let encoded = val.$encode();
                session.tags = session.tags.concat(encoded.tags);
                session.onChange();
            });
        }

        return session;
    }
}
