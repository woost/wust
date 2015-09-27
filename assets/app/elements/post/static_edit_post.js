angular.module("wust.elements").directive("staticEditPost", staticEditPost);

staticEditPost.$inject = [];

function staticEditPost() {
    return {
        restrict: "A",
        templateUrl: "elements/post/static_edit_post.html",
        scope: {
            node: "=",
            alwaysShowTagSuggestions: "@",
            onSave: "&",
            onCancel: "&",
            onDelete: "&"
        },
        controller: StaticEditPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

StaticEditPostCtrl.$inject = ["$state"];

// expects scope.node to be a session.
// used by the scratchpad which retrieves a list of sessions from the EditService.
function StaticEditPostCtrl($state) {
    let vm = this;

    vm.deleteNode = deleteNode;
    vm.saveNode = saveNode;

    function saveNode() {
        vm.node.save().$then(data => vm.onSave({response: data}));
    }

    function deleteNode() {
        vm.node.deleteNode().$then(data => vm.onDelete({response: data}));
    }
}
