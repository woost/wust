angular.module("wust.components").controller("ScratchpadCtrl", ScratchpadCtrl);

ScratchpadCtrl.$inject = ["Search", "HistoryService", "EditService", "DiscourseNode"];

function ScratchpadCtrl(Search, HistoryService, EditService, DiscourseNode) {
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

    vm.searchNodes = searchNodes;
    vm.searchTags = searchTags;
    vm.edit = EditService;
    vm.nodeInfo = DiscourseNode.Post;

    function onEditorLoad(editor) {
        editor.setKeyboardHandler("ace/keyboard/vim");
    }

    function searchNodes(title) {
        return Search.$search({
            title: title,
            label: DiscourseNode.Post.label
        });
    }

    function searchTags(title) {
        console.log("SD");
        return Search.$search({
            title: title,
            label: DiscourseNode.Tag.label
        });
    }
}
