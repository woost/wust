angular.module("wust.elements").directive("postTagChanges", postTagChanges);

postTagChanges.$inject = [];

function postTagChanges() {
    return {
        restrict: "A",
        templateUrl: "elements/post/post_tag_changes.html",
        scope: {
            postTagChanges: "="
        },
        controller: postTagChangesCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

postTagChangesCtrl.$inject = ["RequestsTag"];

function postTagChangesCtrl(RequestsTag) {
    let vm = this;

    vm.upvote = upvote;
    vm.downvote = downvote;

    function upvote(change) {
        RequestsTag.$buildRaw(change).up.$create().$then(val => {
            humane.success("Up voted");
        });
    }

    function downvote(change) {
        RequestsTag.$buildRaw(change).down.$create().$then(val => {
            humane.success("Down voted");
        });
    }
}
