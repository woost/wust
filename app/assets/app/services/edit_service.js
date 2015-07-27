angular.module("wust.services").service("EditService", EditService);

EditService.$inject = ["Post", "HistoryService", "store", "$state", "DiscourseNode"];

function EditService(Post, HistoryService, store, $state, DiscourseNode) {
    let editStore = store.getNamespacedStore("edit");
    let self = this;

    class Session {
        constructor(other) {
            // local id to identify nodes without an id
            this.localId = _.uniqueId();
            this.apply(other);
        }

        apply({
            id, title, description, label, addedTags, original, tags, localId
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

            // initally only show editor if node has description
            this.expandedEditor = !_.isEmpty(description);

            //TODO: why nulls?
            this.addedTags = _.map(_.compact(addedTags) || [], t => {
                if (_.isNumber(t))
                    return t;
                else
                    return t.id;
            });
        }

        dirtyModel() {
            let dirtyModel = _.omit(_.pick(this, _.keys(this.original)), (v, k) => this.original[k] === v);
            if (_.any(this.addedTags))
            //TODO: why do we have nulls here?
                dirtyModel.addedTags = _.compact(this.addedTags);

            // no empty tags array:
            if (_.isEmpty(dirtyModel.tags))
                delete dirtyModel.tags;

            return dirtyModel;
        }

        isPristine(dirtyModel = this.dirtyModel()) {
            return _.isEmpty(dirtyModel);
        }

        isValid(dirtyModel = this.dirtyModel()) {
            return !_.isEmpty(this.title);
        }

        canSave(dirtyModel = this.dirtyModel()) {
            return this.isValid(dirtyModel) && !this.isPristine(dirtyModel);
        }

        save() {
            let dirtyModel = this.dirtyModel();
            if (!this.canSave(dirtyModel))
                return;

            let model = _.pick(this, "id");
            let message = model.id === undefined ? "Added new node" : "Updated now";

            return Post.$buildRaw(model).$update(dirtyModel).$then(data => {
                // DiscourseNode.Post.gotoState(data.id);
                humane.success(message);
                this.apply(data);
                storeEditList();
                //TODO: weird update
                let updateNode = angular.copy(this.tags);
                HistoryService.updateCurrentView(updateNode);
            });
        }

        discard() {
            this.apply(this.original);
            this.addedTags = [];
            this.onChange();
        }

        encode() {
            return this.id === undefined ? _.pick(this, _.keys(this.original)) : this.original;
        }

        addTag(maybeTags) {
            let tag = _.isArray(maybeTags) ? maybeTags[0] : maybeTags;
            if (!tag || _.any(this.tags, {
                id: tag.id
            }))
                return;

            let encoded = tag.encode();
            this.tags.push(encoded);
            this.addedTags.push(encoded.id);
            storeEditList();
        }

        onChange() {
            storeEditList();
        }

        remove() {
            _.remove(self.list, this);
            this.onChange();
        }

        isLocal() {
            return this.id === undefined;
        }

        deleteNode() {
            if (this.isLocal())
                return;

            Post.$buildRaw(_.pick(this, "id")).$destroy().$then(() => {
                HistoryService.remove(this.id);
                this.remove();
                humane.success("Removed node");
                $state.go("browse");
            });
        }
    }

    this.list = restoreEditList();
    this.edit = edit;
    this.updateNode = updateNode;
    this.findNode = findNode;

    function restoreEditList() {
        return _.map(editStore.get("list", self.list) || [], s => new Session(s));
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
        return _.find(this.list, {
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
                if (existingIdx < index)
                    index -= 1;
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
