angular.module("wust.elements").directive("editPost", editPost);

editPost.$inject = [];

function editPost() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/post/edit_post.html",
        scope: {
            node: "=",
            alwaysShowTagSuggestions: "@"
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
            // onBlur: onEditorBlur,
            advanced: {
                highlightActiveLine: false,
                printMarginColumn: false,
                showPrintMargin: false,
                enableSnippets: false,
                enableBasicAutocompletion: false,
                enableLiveAutocompletion: false,
                fontFamily: "inconsolata",
                fontSize: "16px"
            }
        }
    };

    function update(editor) {
        var shouldShow = !editor.session.getValue().length;
        var node = editor.renderer.emptyMessageNode;
        if (!shouldShow && node) {
            editor.renderer.scroller.removeChild(editor.renderer.emptyMessageNode);
            editor.renderer.emptyMessageNode = null;
        } else if (shouldShow && !node) {
            node = editor.renderer.emptyMessageNode = document.createElement("div");
            node.textContent = "Optional description";
            node.className = "ace_invisible ace_emptyMessage";
            node.style.padding = "5px 5px";
            editor.renderer.scroller.appendChild(node);
        }
    }

    function onEditorBlur() {
        vm.node.onChange();
    }

    function onEditorLoad(editor) {
        // editor.setKeyboardHandler("ace/keyboard/vim");
        editor.$blockScrolling = Infinity;
        update(editor);
        editor.on("input", _.partial(update, editor));
    }
}
