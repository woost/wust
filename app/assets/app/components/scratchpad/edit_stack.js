angular.module("wust.components").service("EditStack", EditStack);

EditStack.$inject = ["Post", "DiscourseNode", "NodeHistory"];

function EditStack(Post, DiscourseNode, NodeHistory) {
    let self = this;

    let editNode = null;

    self.session = {
        title: "",
        description: ""
    };

    self.stack = [];

    self.saveNode = saveNode;
    self.editExisting = editExisting;
    self.editNew = editNew;
    self.removeEdited = removeEdited;

    function buildNewPost() {
        return Post.$build({
            title: self.session.title,
            description: self.session.description
        });
    }

    function switchEdit(node) {
        if (!editNode && (self.session.title === "") && (self.session.description === "")) {
            if (!node)
                return;
        } else {
            if (!editNode) {
                editNode = buildNewPost();
                self.stack.push(editNode);
            }

            editNode.title = self.session.title;
            editNode.description = self.session.description;
        }

        editNode = node;
        self.session.title = node ? node.title : "";
        self.session.description = node ? node.description : "";
    }

    function editNew() {
        switchEdit();
    }

    function editExisting(nodes) {
        //TODO: we get an array if multiple nodes were in completion and enter was pressed
        let node = _.isArray(nodes) ? nodes[0] : nodes;
        let existing = _.find(self.stack, (node.id !== undefined) ? {
            id: node.id
        } : node);
        if (existing) {
            switchEdit(existing);
        } else {
            //TODO: translation between models, should be the same?
            node = Post.$collection().$buildRaw(node).$reveal();
            switchEdit(node);
            self.stack.push(node);
            if (node.id !== undefined) {
                NodeHistory.add(node);
            }
        }

    }

    function removeEdited(node) {
        if (editNode === node) {
            switchEdit();
        }

        _.remove(self.stack, node);
    }

    function saveNode() {
        let node = editNode;
        if (node) {
            node.title = self.session.title;
            node.description = self.session.description;
        } else {
            node = buildNewPost();
        }

        node.$update().$then(data => {
            humane.success("Added new node");
            DiscourseNode.Post.gotoState(data.id);
            _.remove(self.stack, node);
            editNode = null;
            self.session.title = "";
            self.session.description = "";
            switchEdit();
        });
    }

}
