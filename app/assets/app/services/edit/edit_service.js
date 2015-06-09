angular.module("wust.services").service("EditService", EditService);

EditService.$inject = ["Post", "HistoryService", "store"];

function EditService(Post, HistoryService, store) {
    let editStore = store.getNamespacedStore("edit");
    let self = this;

    class Session {
        constructor({
            id, title, description, tags, addedTags, original
        }) {
            this.id = id;
            this.title = title || "";
            this.description = description || "";
            this.tags = angular.copy(tags) || [];
            this.addedTags = addedTags || [];
            this.original = original || {
                title: this.title,
                description: this.description
            };
        }

        dirtyModel() {
            let dirtyModel = _.omit(_.pick(this, _.keys(this.original)), (v,k) => this.original[k] === v);
            if (_.any(this.addedTags))
                dirtyModel.addedTags = _.map(this.addedTags, t => t.id);

            return dirtyModel;
        }

        isPristine(dirtyModel = this.dirtyModel()) {
            return _.isEmpty(dirtyModel);
        }

        save() {
            let dirtyModel = this.dirtyModel();
            if (this.isPristine(dirtyModel))
                return;

            Post.$buildRaw(_.pick(this, "id")).$update(dirtyModel).$then(data => {
                humane.success("Added new node");
                this.remove();
            });
        }

        addTag(maybeTags) {
            let tag = _.isArray(maybeTags) ? maybeTags[0] : maybeTags;
            if (!tag || _.any(this.tags, {
                id: tag.id
            }))
                return;

            tag = _.pick(tag, "id", "title", "description");
            this.tags.push(tag);
            this.addedTags.push(tag);
        }

        onChange() {
            storeStack();
        }

        remove() {
            _.remove(self.stack, this);
            this.onChange();
        }
    }

    this.stack = restoreStack();
    this.edit = edit;

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

        if (!existing) {
            existing = new Session(node);
            self.stack.push(existing);
            storeStack();
        }

        return existing;
    }

    function edit(maybeNodes) {
        //TODO: we get an array if multiple nodes were in completion and enter was pressed
        let node = _.isArray(maybeNodes) ? maybeNodes[0] : maybeNodes;
        assureSessionExists(node);
    }
}
