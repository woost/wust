angular.module("wust.services").service("EditService", EditService);

EditService.$inject = ["Post", "Connectable", "HistoryService", "store", "DiscourseNode", "ZenService"];

function EditService(Post, Connectable, HistoryService, store, DiscourseNode, ZenService) {
    let editStore = store.getNamespacedStore("edit");
    let self = this;

    class Session {
        constructor(other, lazyAdd = false, connectable = false, newDiscussion = false) {
            // local id to identify nodes without an id
            this.localId = _.uniqueId();

            this.apply(other);

            this.expandedEditor = !!other.expandedEditor;

            this.service = connectable ? Connectable : Post;

            this.lazyAdd = lazyAdd;

            this.newDiscussion = newDiscussion;
        }

        apply({
            id, title, description, label, original, tags, localId, referenceNode, newDiscussion, isHyperRelation
        }) {
            tags = tags || [];
            this.id = id;
            this.localId = localId === undefined ? this.localId : localId;
            this.title = title || "";
            this.title = title || "";
            this.description = description || "";
            this.label = label;
            this.referenceNode = referenceNode;
            this.newDiscussion = newDiscussion || false;
            this.isHyperRelation = isHyperRelation || false;
            this.tags = angular.copy(tags.map(t => t.$encode ? t.$encode() : t));
            this.original = original || {
                id: this.id,
                localId: this.localId,
                title: this.title,
                description: this.description,
                label: this.label,
                tags: angular.copy(tags.map(t => t.$encode ? t.$encode() : t))
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
            let addedTags = _.reject(this.tags, t => t.id && _.any(this.original.tags, _.pick(t, "id"))).map(t => t.id ? _.pick(t, "id") : _.pick(t, "title"));
            let removedTags = _.reject(this.original.tags, t => !t.id || _.any(this.tags, _.pick(t, "id"))).map(t => t.id);

            if (addedTags.length > 0)
                dirtyModel.addedTags = addedTags;
            if (removedTags.length > 0)
                dirtyModel.removedTags = removedTags;

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
            let message = model.id === undefined ? "Added new node" : "Updated now";

            let referenceNode = this.referenceNode;
            let promise;
            if (referenceNode) {
                promise = connectNodes(this.encode(), referenceNode, dirtyModel);
            } else {
                promise = this.service.$buildRaw(model).$update(dirtyModel);
            }

            promise.$then(data => {
                humane.success(message);

                this.apply(data);
                storeEditList();

                if (referenceNode === undefined) {
                    HistoryService.updateCurrentView(this.encode());
                }
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
            this.onChange();
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
    this.createSession = createSession;
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
        if (existingIdx >= 0) {
            existing = self.list[existingIdx];
            self.list.splice(existingIdx, 1);
        } else {
            existing = new Session(node);
        }

        self.list.splice(index, 0, existing);
        storeEditList();
        return existing;
    }

    function connectNodes(startNode, endNode, saveNode = startNode) {
        let localStart = startNode.id === undefined;
        let start = Connectable.$buildRaw(_.pick(endNode, "id"));
        let promise = localStart ? start.connectsFrom.$create(saveNode) : start.connectsFrom.$buildRaw(_.pick(startNode, "id")).$save({});

        promise.$then(response => {
            HistoryService.addConnectToCurrentView(endNode.id, response);
        });

        return promise;
    }

    function createSession(node = {}, lazyAdd = true, connectable = false, newDiscussion = false) {
        return new Session(node, lazyAdd, connectable, newDiscussion);
    }

    function editSession(session, index = 0) {
        self.list.splice(index, 0, session);
    }

    function editAnswer(node) {
        let existingAnswer = _.find(self.list, elem => elem.isLocal && elem.referenceNode && elem.referenceNode.id === node.id);
        if (existingAnswer === undefined) {
            let session = createSession({}, true, false, false);
            session.setReference(node);
            return session;
        } else {
            return edit(existingAnswer);
        }
    }

    function editNewDiscussion(tags = []) {
        let existingAnswer = _.find(self.list, elem => elem.isLocal && !elem.referenceNode && elem.newDiscussion && _.every(tags, tag => _.any(elem.tags, other => other.id === tag.id)));
        if (existingAnswer === undefined) {
            return createSession({tags}, true, false, true);
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
