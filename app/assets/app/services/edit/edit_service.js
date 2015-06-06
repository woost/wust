angular.module("wust.services").service("EditService", EditService);

EditService.$inject = ["Post", "HistoryService", "store"];

function EditService(Post, HistoryService, store) {
    let editStore = store.getNamespacedStore("edit");
    let self = this;

    this.stack = editStore.get("stack") || [];

    class Session {
        constructor({
            id, title, description, tags
        }) {
            this.id = id;
            this.title = title || "";
            this.description = description || "";
            this.tags = tags || [];
            this.addedTags = [];
        }

        save() {
            let node = _.pick(this, "id", "title", "description");
            node.addedTags = _.map(node.addedTags, t => t.id);

            Post.$buildRaw(node).$update().$then(data => {
                humane.success("Added new node");
            });
        }

        addTag(maybeTags) {
            let tag = _.isArray(maybeTags) ? maybeTags[0] : maybeTags;
            if (!tag || !_.any(this.tags, {
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


    this.edit = edit;

    function storeStack() {
        editStore.set("stack", self.stack);
    }

    function assureSessionExists(node) {
        let existing = _.find(self.stack, {
            id: node.id
        });

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
        node = _.pick(node, "id", "title", "description", "tags");
        assureSessionExists(node);
    }
}
