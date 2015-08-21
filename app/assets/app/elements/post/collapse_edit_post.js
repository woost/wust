angular.module("wust.elements").directive("collapseEditPost", collapseEditPost);

collapseEditPost.$inject = [];

function collapseEditPost() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/post/collapse_edit_post.html",
        scope: {
            node: "="
        },
        controller: collapseEditPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

collapseEditPostCtrl.$inject = [];

// expects scope.node to be a session.
// used by the scratchpad which retrieves a list of sessions from the EditService.
function collapseEditPostCtrl() {
    let vm = this;

    vm.focusEditTags = false;
    vm.editableChange = editableChange;
    vm.redirectEnter = redirectEnter;
    vm.save = save;

    function redirectEnter(event) {
        if(event.keyCode === 13) {
            vm.focusEditTags = true;
            event.stopPropagation();
            event.preventDefault();
        }
    }

    function save() {
        if (vm.tagSearch) {
            vm.node.tags.push({title: vm.tagSearch});
            vm.tagSearch = "";
        }

        vm.node.save();
    }

    function editableChange(data) {
        vm.node.title = data;
        vm.node.onChange();
    }
}
