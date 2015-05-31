angular.module("wust.components").controller("ScratchpadCtrl", ScratchpadCtrl);

ScratchpadCtrl.$inject = ["Search", "NodeHistory", "EditStack", "DiscourseNode"];

function ScratchpadCtrl(Search, NodeHistory, EditStack, DiscourseNode) {
    let vm = this;

    vm.ace = {
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

    vm.visitedNodes = NodeHistory.visited;
    vm.searchNodes = searchNodes;
    vm.editStack = EditStack;
    vm.nodeInfo = DiscourseNode.Post;

    function onEditorLoad(editor) {
        editor.setKeyboardHandler("ace/keyboard/vim");
    }

    function searchNodes(title) {
        return Search.$search({
            title: title
        });
    }
}
