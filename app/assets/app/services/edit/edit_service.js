angular.module("wust.services").service("EditService", EditService);

EditService.$inject = ["Post", "NodeHistory", "store"];

function EditService(Post, NodeHistory, store) {
    let editStore = store.getNamespacedStore("edit");
    let self = this;


    this.session = editStore.get("session") || {
        index: -1,
        title: "",
        description: ""
    };

    this.stack = editStore.get("stack") || [];

    this.saveNode = saveNode;
    this.editExisting = editExisting;
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
                description: node.description
            };
        } else {
            self.session = {
                index: -1,
                title: "",
                description: ""
            };
        }
        onChange();
    }

    function switchEdit(node) {
        if (self.session.index >= 0) {
            self.stack[self.session.index].title = self.session.title;
            self.stack[self.session.index].description = self.session.description;
            editStore.set("stack", self.stack);
        } else if ((self.session.title !== "") || (self.session.description !== "")) {
            pushStack(_.pick(self.session, "title", "description"));
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
        //TODO: might be a restmod resource
        if (node.$then !== undefined) {
            NodeHistory.add(node);
        }

        node = _.pick(node, "id", "title", "description");
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
        let node = oldNode || {};
        node.title = self.session.title;
        node.description = self.session.description;

        Post.$buildRaw(node).$update().$then(data => {
            humane.success("Added new node");
            removeStack(oldNode);
        });
    }

}
