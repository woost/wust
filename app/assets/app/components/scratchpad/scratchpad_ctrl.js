angular.module("wust.components").controller("ScratchpadCtrl", ScratchpadCtrl);

ScratchpadCtrl.$inject = ["Post", "DiscourseNode", "NodeHistory", "Search"];

function ScratchpadCtrl(Post, DiscourseNode, NodeHistory, Search) {
    let vm = this;

    vm.visitedNodes = NodeHistory.visited;

    vm.ace = {
        title: "",
        description: "",
        editNode: null,
        options: {
            useWrapMode: true,
            showGutter: false,
            mode: "markdown",
            require: ["ace/ext/language_tools"],
            onLoad: onEditorLoad,
            advanced: {
                printMarginColumn: false,
                enableSnippets: true,
                enableBasicAutocompletion: true,
                enableLiveAutocompletion: true
            }
        }
    };

    vm.editStack = [];

    vm.nodeInfo = DiscourseNode.Post;
    vm.saveNode = saveNode;
    vm.editExisting = editExisting;
    vm.editNew = editNew;
    vm.removeEdited = removeEdited;
    vm.searchNodes = searchNodes;

    function onEditorLoad(editor) {
        editor.setKeyboardHandler("ace/keyboard/vim");
    }

    function buildNewPost() {
        return Post.$build({
            title: vm.ace.title,
            description: vm.ace.description
        });
    }

    function switchEdit(node) {
        if (!vm.ace.editNode && (vm.ace.title === "") && (vm.ace.description === "")) {
            if (!node)
                return;
        } else {
            if (!vm.ace.editNode) {
                vm.ace.editNode = buildNewPost();
                vm.editStack.push(vm.ace.editNode);
            }

            vm.ace.editNode.title = vm.ace.title;
            vm.ace.editNode.description = vm.ace.description;
        }

        vm.ace.editNode = node;
        vm.ace.title = node ? node.title : "";
        vm.ace.description = node ? node.description : "";
    }

    function editNew() {
        switchEdit();
    }

    function editExisting(nodes) {
        //TODO: we get an array if multiple nodes were in completion and enter was pressed
        let node = _.isArray(nodes) ? nodes[0] : nodes;
        let existing = _.find(vm.editStack, (node.id !== undefined) ? {
            id: node.id
        } : node);
        if (node.id !== undefined) {
            NodeHistory.add(node);
        }
        if (existing) {
            switchEdit(existing);
        } else {
            //TODO: translation between models, should be the same?
            node = Post.$collection().$buildRaw(node).$reveal();
            switchEdit(node);
            vm.editStack.push(node);
        }

    }

    function removeEdited(node) {
        if (vm.ace.editNode === node) {
            switchEdit();
        }

        _.remove(vm.editStack, node);
    }

    function saveNode() {
        let node = vm.ace.editNode;
        if (node) {
            node.title = vm.ace.title;
            node.description = vm.ace.description;
        } else {
            node = buildNewPost();
        }

        node.$update().$then(data => {
            humane.success("Added new node");
            DiscourseNode.Post.gotoState(data.id);
            _.remove(vm.editStack, node);
            vm.editNode = null;
            vm.ace.title = "";
            vm.ace.description = "";
            switchEdit();
        });
    }

    function searchNodes(title) {
        return Search.$search({
            title: title
        });
    }
}
