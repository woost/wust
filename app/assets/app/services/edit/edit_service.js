angular.module("wust.services").service("EditService", EditService);

EditService.$inject = ["Post", "HistoryService", "store"];

function EditService(Post, HistoryService, store) {
    let editStore = store.getNamespacedStore("edit");
    let self = this;

    this.session = editStore.get("session");
    if (!this.session)
        setSession();

    this.stack = editStore.get("stack") || [];

    this.saveNode = saveNode;
    this.editExisting = editExisting;
    this.addTag = addTag;
    this.editNew = editNew;
    this.removeEdited = removeEdited;
    this.onChange = onChange;

    function onChange() {
        editStore.set("session", self.session);
    }

    function pushStack(node) {
        self.stack.push(node);
        editStore.set("stack", self.stack);
    }

    function removeStack(node) {
        if (self.stack[self.session.index] === node) {
            editNew();
        }

        _.remove(self.stack, node);
        editStore.set("stack", self.stack);
    }

    function setSession(node) {
        if (node) {
            let index = findExistingIndex(node);
            if (index === -1) {
                index = self.stack.length;
                pushStack(node);
            }

            self.session = {
                index,
                title: node.title,
                description: node.description,
                tags: node.tags,
                addedTags: node.addedTags || []
            };
        } else {
            self.session = {
                index: -1,
                title: "",
                description: "",
                tags: [],
                addedTags: []
            };
        }
        onChange();
    }

    function addTag(maybeTags) {
        let tag = _.isArray(maybeTags) ? maybeTags[0] : maybeTags;
        if (!tag)
            return;

        let tmp = _.pick(tag, "id", "title", "description");
        if (!_.any(self.session.tags, {
            id: tmp.id
        })) {
            self.session.tags.push(tmp);
            self.session.addedTags.push(tmp);
            onChange();
        }
    }

    function switchEdit(node) {
        if (self.session.index >= 0) {
            self.stack[self.session.index].title = self.session.title;
            self.stack[self.session.index].description = self.session.description;
            self.stack[self.session.index].tags = self.session.tags;
            editStore.set("stack", self.stack);
        } else if ((self.session.title !== "") || (self.session.description !== "") || _.any(self.session.tags)) {
            pushStack(_.pick(self.session, "title", "description", "tags"));
        }

        setSession(node);
    }

    function findExistingIndex(node) {
        return _.findIndex(self.stack, (node.id === undefined) ? node : {
            id: node.id
        });
    }

    function editNew() {
        switchEdit();
    }

    function editExisting(maybeNodes) {
        //TODO: we get an array if multiple nodes were in completion and enter was pressed
        let node = _.isArray(maybeNodes) ? maybeNodes[0] : maybeNodes;
        if (!node)
            return;
        //TODO: might be a restmod resource
        if (node.$then !== undefined) {
            HistoryService.add(node);
        }

        node = _.pick(node, "id", "title", "description", "tags");
        let existingIndex = findExistingIndex(node);
        switchEdit(node);
    }

    function removeEdited(node) {
        if (this.stack[self.session.index] === node) {
            editNew();
        }

        removeStack(node);
    }

    function saveNode() {
        let oldNode = self.stack[self.session.index];
        let node = _.pick(self.session, "title", "description", "addedTags");
        node.addedTags = _.map(node.addedTags, t => t.id);
        node.id = _.get(oldNode, "id");

        Post.$buildRaw(node).$update().$then(data => {
            humane.success("Added new node");
            removeStack(oldNode);
        });
    }

}
