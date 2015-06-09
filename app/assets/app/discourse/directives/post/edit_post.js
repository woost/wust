angular.module("wust.discourse").directive("editPost", editPost);

editPost.$inject = ["DiscourseNode"];

function editPost(DiscourseNode) {
    return {
        restrict: "A",
        templateUrl: "assets/app/discourse/directives/post/edit_post.html",
        scope: {
            node: "="
        },
        controller: editPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

editPostCtrl.$inject = ["Search", "DiscourseNode"];

// expects scope.node to be a session.
// used by the scratchpad which retrieves a list of sessions from the EditService.
function editPostCtrl(Search, DiscourseNode) {
    let vm = this;

    vm.ace = {
        options: {
            useWrapMode: true,
            showGutter: false,
            mode: "markdown",
            require: ["ace/ext/language_tools"],
            onLoad: onEditorLoad,
            onBlur: vm.node.onChange,
            advanced: {
                printMarginColumn: false,
                enableSnippets: true,
                enableBasicAutocompletion: true,
                enableLiveAutocompletion: true
            }
        }
    };

    vm.searchTags = searchTags;
    vm.nodeInfo = DiscourseNode.Post;

    function onEditorLoad(editor) {
        editor.setKeyboardHandler("ace/keyboard/vim");
        editor.$blockScrolling = Infinity;
    }

    function searchTags(title) {
        return Search.$search({
            title: title,
            label: DiscourseNode.Tag.label
        });
    }
}
