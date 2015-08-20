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

editPostCtrl.$inject = ["$state"];

// expects scope.node to be a session.
// used by the scratchpad which retrieves a list of sessions from the EditService.
function editPostCtrl($state) {
    let vm = this;

    vm.redirectEnter = redirectEnter;
    vm.focusEditTags = false;

    function redirectEnter(event) {
        if(event.keyCode === 13) {
            vm.focusEditTags = true;
            event.stopPropagation();
            event.preventDefault();
        }
    }
}
