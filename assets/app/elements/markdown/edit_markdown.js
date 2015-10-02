angular.module("wust.elements").directive("editMarkdown", editMarkdown);

editMarkdown.$inject = [];

function editMarkdown() {
    return {
        restrict: "A",
        templateUrl: "elements/markdown/edit_markdown.html",
        scope: {
            editMarkdown: "=",
            onChange: "="
        },
        controller: editMarkdownCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

editMarkdownCtrl.$inject = [];

// expects scope.node to be a session.
// used by the scratchpad which retrieves a list of sessions from the EditService.
function editMarkdownCtrl() {
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
                // printMarginColumn: false,
                showPrintMargin: false,
                // enableSnippets: false,
                // enableBasicAutocompletion: false,
                // enableLiveAutocompletion: false,
                fontFamily: "Monospace",
                fontSize: "16px",
                maxLines: 20,
                minLines: 10
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
            node.style.padding = "10px";
            editor.renderer.scroller.appendChild(node);
        }
    }

    function onEditorBlur() {
        vm.onChange();
    }

    function onEditorLoad(editor) {
        // editor.setKeyboardHandler("ace/keyboard/vim");
        editor.$blockScrolling = Infinity;
        editor.renderer.setPadding(10);
        update(editor);
        editor.on("input", _.partial(update, editor));
    }
}
