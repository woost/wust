angular.module("wust.services").service("EditService", EditService);

EditService.$inject = ["Post", "HistoryService", "store", "$state", "DiscourseNode"];

function EditService(Post, HistoryService, store, $state, DiscourseNode) {
    let editStore = store.getNamespacedStore("edit");
    let self = this;

    class Session {
        constructor(other) {
            this.apply(other);
            this.tags = angular.copy(other.tags) || [];
        }

        apply({
            id, title, description, addedTags, original
        }) {
            this.id = id;
            this.title = title || "";
            this.description = description || "";
            this.original = original || {
                title: this.title,
                description: this.description
            };

            //TODO: stupid having sometimes numbers and sometimes objects...
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
            //TODO: why do we have nulls here???

            if (_.any(this.addedTags))
                //TODO: why do we have nulls here?
                dirtyModel.addedTags = _.compact(this.addedTags);

            return dirtyModel;
        }

        isPristine(dirtyModel = this.dirtyModel()) {
            return _.isEmpty(dirtyModel);
        }

        save() {
            let dirtyModel = this.dirtyModel();
            if (this.isPristine(dirtyModel))
                return;

            let model = _.pick(this, "id");
            let message = model.id === undefined ? "Added new node" : "Updated now";
            Post.$buildRaw(model).$update(dirtyModel).$then(data => {
                DiscourseNode.Post.gotoState(data.id);
                humane.success(message);
                this.apply(data);
                storeStack();
                //TODO: weird update
                if (HistoryService.currentViewNode.id === data.id) {
                    _.assign(HistoryService.currentViewNode, _.omit(data, "tags"));
                    //response has empty tags array
                    HistoryService.currentViewNode.tags = angular.copy(this.tags);
                }
            });
        }

        addTag(maybeTags) {
            let tag = _.isArray(maybeTags) ? maybeTags[0] : maybeTags;
            if (!tag || _.any(this.tags, {
                id: tag.id
            }))
                return;

            let encoded = tag.$encode();
            this.tags.push(encoded);
            this.addedTags.push(encoded.id);
            storeStack();
        }

        onChange() {
            storeStack();
        }

        remove() {
            _.remove(self.stack, this);
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

    this.stack = restoreStack();
    this.edit = edit;

    //TODO: should not be exposed!! part of hack in discourse_node_list
    this.storeStack = storeStack;

    function restoreStack() {
        return _.map(editStore.get("stack", self.stack) || [], s => new Session(s));
    }

    function storeStack() {
        editStore.set("stack", self.stack);
    }

    function assureSessionExists(node = {}) {
        let existing = node.id !== undefined ? _.find(self.stack, {
            id: node.id
        }) : undefined;

        if (existing) {
            _.remove(self.stack, existing);
        } else {
            existing = new Session(node);
        }

        self.stack.push(existing);
        storeStack();
        return existing;
    }

    function edit(maybeNodes) {
        //TODO: we get an array if multiple nodes were in completion and enter was pressed
        let node = _.isArray(maybeNodes) ? maybeNodes[0] : maybeNodes;
        assureSessionExists(node);
    }
}
