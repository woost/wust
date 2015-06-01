angular.module("wust.components").controller("ScratchpadCtrl", ScratchpadCtrl);

ScratchpadCtrl.$inject = ["Search", "NodeHistory", "EditService", "DiscourseNode"];

function ScratchpadCtrl(Search, NodeHistory, EditService, DiscourseNode) {
    let vm = this;

    vm.ace = {
        options: {
            useWrapMode: true,
            showGutter: false,
            mode: "markdown",
            require: ["ace/ext/language_tools"],
            onLoad: onEditorLoad,
            onBlur: EditService.onChange,
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
    vm.edit = EditService;
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
