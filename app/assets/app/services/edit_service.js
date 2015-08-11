angular.module("wust.services").service("EditService", EditService);

EditService.$inject = ["Post", "HistoryService", "store", "DiscourseNode"];

function EditService(Post, HistoryService, store, DiscourseNode) {
    let editStore = store.getNamespacedStore("edit");
    let self = this;

    class Session {
        constructor(other) {
            // local id to identify nodes without an id
            this.localId = _.uniqueId();
            this.apply(other);

            // initally only show editor if node has description
            this.expandedEditor = !_.isEmpty(this.description);
        }

        apply({
            id, title, description, label, original, tags, localId
        }) {
            tags = tags || [];
            this.id = id;
            this.localId = localId === undefined ? this.localId : localId;
            this.title = title || "";
            this.description = description || "";
            this.label = label;
            this.tags = angular.copy(tags);
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

        //TODO: we should rewrite the whole logic here, it is weird and hacky, but it works so i leave it as is :)
        dirtyModel() {
            let dirtyModel = _.omit(_.pick(this, _.keys(this.original)), (v, k) => this.original[k] === v);
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
            let dirtyModel = this.dirtyModel();
            if (!this.canSave)
                return;

            // we do this in order to disable the form until we saved the node.
            this.unsetValidityProperties();

            let model = _.pick(this, "id");
            let message = model.id === undefined ? "Added new node" : "Updated now";

            return Post.$buildRaw(model).$update(dirtyModel).$then(data => {
                // DiscourseNode.Post.gotoState(data.id);
                humane.success(message);

                // the response only holds newly added tags
                data.tags = this.tags.filter(t => t.id && !_.any(dirtyModel.addedTags, {id: t.id})).concat(data.tags).map(t => t.encode ? t.encode() : t);
                this.apply(data);
                storeEditList();

                //TODO: weird update
                let updateNode = angular.copy(data);
                HistoryService.updateCurrentView(updateNode);
            }, () => this.setValidityProperties());
        }

        discard() {
            this.apply(this.original);
            this.onChange();
        }

        encode() {
            return this.id === undefined ? _.pick(this, _.keys(this.original)) : this.original;
        }

        //TODO: needed?
        addTag(maybeTags) {
            let tag = _.isArray(maybeTags) ? maybeTags[0] : maybeTags;
            if (!tag || _.any(this.tags, {
                id: tag.id
            }))
                return;

            let encoded = tag.encode ? tag.encode() : tag;
            this.tags.push(encoded);

            this.onChange();
        }

        onChange() {
            this.setValidityProperties();
            storeEditList();
        }

        setValidityProperties() {
            let dirtyModel = this.dirtyModel();
            this.isPristine = _.isEmpty(dirtyModel);
            this.isValid = !_.isEmpty(this.title);
            this.canSave = this.isValid && !this.isPristine;
            this.isLocal = this.id === undefined;
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
    this.updateNode = updateNode;
    this.findNode = findNode;
    this.persist = storeEditList;
    this.forget = clearEditList;

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
        let existing;
        if (node === undefined) {
            // fresh session
            existing = new Session({});
        } else {
            let searchFor = node.id === undefined ? "localId" : "id";
            // add existing node for editing
            let existingIdx = _.findIndex(self.list, _.pick(node, searchFor));
            if (existingIdx >= 0) {
                existing = self.list[existingIdx];
                self.list.splice(existingIdx, 1);
            } else {
                existing = new Session(node);
            }
        }

        self.list.splice(index, 0, existing);
        storeEditList();
        return existing;
    }

    function edit(maybeNodes, index = 0) {
        //TODO: we get an array if multiple nodes were in completion and enter was pressed
        let node = _.isArray(maybeNodes) ? maybeNodes[0] : maybeNodes;
        return assureSessionExists(node, index);
    }
}
