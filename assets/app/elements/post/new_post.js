angular.module("wust.elements").directive("newPost", newPost);

newPost.$inject = [];

function newPost() {
    return {
        restrict: "A",
        templateUrl: "elements/post/new_post.html",
        scope: {
            node: "=",
            inputFocus: "=",
            alwaysShowTagSuggestions: "@"
        },
        controller: newPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

newPostCtrl.$inject = ["$state"];

// expects scope.node to be a session.
// used by the scratchpad which retrieves a list of sessions from the EditService.
function newPostCtrl($state) {
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
