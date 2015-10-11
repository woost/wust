angular.module("wust.elements").directive("collapseEditPost", collapseEditPost);

collapseEditPost.$inject = [];

function collapseEditPost() {
    return {
        restrict: "A",
        templateUrl: "elements/post/collapse_edit_post.html",
        scope: {
            node: "="
        },
        controller: collapseEditPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

collapseEditPostCtrl.$inject = ["Auth"];

// expects scope.node to be a session.
// used by the scratchpad which retrieves a list of sessions from the EditService.
function collapseEditPostCtrl(Auth) {
    let vm = this;

    vm.focusEditTags = false;
    vm.editableChange = editableChange;
    vm.redirectEnter = redirectEnter;
    vm.save = save;
    vm.Auth = Auth;

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

        vm.node.save(true);
    }

    function editableChange(data) {
        vm.node.title = data;
        vm.node.onChange();
    }
}
