angular.module("wust.elements").directive("staticEditPost", staticEditPost);

staticEditPost.$inject = [];

function staticEditPost() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/post/static_edit_post.html",
        scope: {
            node: "="
        },
        controller: StaticEditPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

StaticEditPostCtrl.$inject = [];

// expects scope.node to be a session.
// used by the scratchpad which retrieves a list of sessions from the EditService.
function StaticEditPostCtrl() {
    let vm = this;

    vm.editableChange = editableChange;
    vm.redirectEnter = redirectEnter;
    vm.focusEditTags = false;

    function redirectEnter(event) {
        if(event.keyCode === 13) {
            vm.focusEditTags = true;
            event.stopPropagation();
            event.preventDefault();
        }
    }

    function editableChange(data) {
        vm.node.title = data;
        vm.node.onChange();
    }
}