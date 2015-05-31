angular.module("wust.components").controller("ScratchpadCtrl", ScratchpadCtrl);

ScratchpadCtrl.$inject = ["Post", "DiscourseNode"];

function ScratchpadCtrl(Post, DiscourseNode) {
    let vm = this;

    vm.aceOptions = {
        useWrapMode: true,
        showGutter: false,
        mode: "markdown",
        require: ["ace/ext/language_tools"],
        onLoad,
        advanced: {
            printMarginColumn: false,
            enableSnippets: true,
            enableBasicAutocompletion: true,
            enableLiveAutocompletion: true
        }
    };

    vm.addNode = addNode;
    vm.newNode = newNode();

    function onLoad(editor) {
        editor.setKeyboardHandler("ace/keyboard/vim");
    }

    function newNode() {
        return Post.$build({
            title: "",
            description: ""
        });
    }

    function addNode() {
        vm.newNode.$save().$then(data => {
            humane.success("Added new node");
            DiscourseNode.Post.gotoState(data.id);
            vm.newNode = newNode();
        });
    }
}
