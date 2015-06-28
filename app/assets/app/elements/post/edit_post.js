angular.module("wust.elements").directive("editPost", editPost);

editPost.$inject = [];

function editPost() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/post/edit_post.html",
        scope: {
            node: "="
        },
        controller: editPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

editPostCtrl.$inject = ["DiscourseNode"];

// expects scope.node to be a session.
// used by the scratchpad which retrieves a list of sessions from the EditService.
function editPostCtrl(DiscourseNode) {
    let vm = this;

    vm.ace = {
        options: {
            useWrapMode: true,
            showGutter: false,
            mode: "markdown",
            require: ["ace/ext/language_tools"],
            onLoad: onEditorLoad,
            onBlur: onEditorBlur,
            advanced: {
                highlightActiveLine: false,
                printMarginColumn: false,
                showPrintMargin: false,
                enableSnippets: false,
                enableBasicAutocompletion: false,
                enableLiveAutocompletion: false
            }
        }
    };

    function onEditorBlur() {
        vm.node.onChange();
    }

    function onEditorLoad(editor) {
        // editor.setKeyboardHandler("ace/keyboard/vim");
        editor.$blockScrolling = Infinity;
    }
}