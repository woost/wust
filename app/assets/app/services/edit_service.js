angular.module("wust.services").service("EditService", EditService);

EditService.$inject = ["Post", "HistoryService", "store", "DiscourseNode", "ZenService"];

function EditService(Post, HistoryService, store, DiscourseNode, ZenService) {
    let editStore = store.getNamespacedStore("edit");
    let self = this;

    class Session {
        constructor(other, lazyAdd = false) {
            // local id to identify nodes without an id
            this.localId = _.uniqueId();

            this.apply(other);

            this.expandedEditor = other.expandedEditor === undefined ? true : other.expandedEditor;

            this.lazyAdd = lazyAdd;
        }

        apply({
            id, title, description, label, original, tags, localId, referenceNode
        }) {
            tags = tags || [];
            this.id = id;
            this.localId = localId === undefined ? this.localId : localId;
            this.title = title || "";
            this.title = title || "";
            this.description = description || "";
            this.label = label;
            this.tags = angular.copy(tags);
            this.referenceNode = referenceNode;
            this.original = original || {
                id: this.id,
                localId: this.localId,
                title: this.title,
                description: this.description,
                label: this.label,
                tags: tags
            };

            this.setValidityProperties();
        }

        updateOriginal() {
            this.apply(_.omit(this, "original"));
        }

        setReference(reference) {
            this.referenceNode = reference.encode ? reference.encode() : reference;
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
            if (!this.canSave)
                return;

            // we do this in order to disable the form until we saved the node.
            this.unsetValidityProperties();

            let model = _.pick(this, "id");
            let message = model.id === undefined ? "Added new node" : "Updated now";

            let referenceNode = this.referenceNode;
            Post.$buildRaw(model).$update(dirtyModel).$then(data => {
                humane.success(message);

                // the response only holds newly added tags
                data.tags = this.tags.filter(t => t.id && !_.any(dirtyModel.addedTags, {id: t.id})).concat(data.tags).map(t => t.encode ? t.encode() : t);
                this.apply(data);
                storeEditList();

                //TODO should create+connect in one go...
                if (referenceNode === undefined) {
                    HistoryService.updateCurrentView(this.encode());
                } else {
                    connectNodes(this.encode(), referenceNode);
                }
            }, () => this.setValidityProperties());
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

        setValidityProperties() {
            let dirtyModel = this.dirtyModel();
            this.isPristine = _.isEmpty(dirtyModel);
            this.isLocal = this.id === undefined;
            this.isValid = !_.isEmpty(this.title);
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

            Post.$buildRaw(_.pick(this, "id")).$destroy().$then(() => {
                HistoryService.remove(this.id);
                this.remove();
                humane.success("Removed node");
            });
        }
    }

    this.list = restoreEditList();
    this.edit = edit;
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

    function connectNodes(startNode, endNode) {
        let ref;
        if (endNode.isHyperRelation) {
            let start = Post.$buildRaw({
                id: endNode.startId
            });
            let hyper = start.connectsTo.$buildRaw({
                id: endNode.endId
            });
            ref = hyper.connectsFrom.$buildRaw(_.pick(startNode, "id"));
        } else {
            let start = Post.$buildRaw(_.pick(startNode, "id"));
            ref = start.connectsTo.$buildRaw(_.pick(endNode, "id"));
        }
        ref.$save({}).$then(response => {
            humane.success("Connected node");
            // add the infos we got from the node parameter
            let startResponse = _.find(response.graph.nodes, _.pick(startNode, "id"));
            _.assign(startResponse, startNode);
            _.assign(response.node, startNode);

            HistoryService.addConnectToCurrentView(endNode.id, response);
        });
    }

    function createSession() {
        return new Session({}, true);
    }

    function editSession(session, index = 0) {
        session.updateOriginal();
        self.list.splice(index, 0, session);
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
