angular.module("wust.elements").directive("postEditChanges", postEditChanges);

postEditChanges.$inject = [];

function postEditChanges() {
    return {
        restrict: "A",
        templateUrl: "elements/post/post_edit_changes.html",
        scope: {
            postEditChanges: "="
        },
        controller: postEditChangesCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

postEditChangesCtrl.$inject = ["RequestsEdit"];

function postEditChangesCtrl(RequestsEdit) {
    let vm = this;

    vm.upvote = upvote;
    vm.downvote = downvote;

    function upvote(change) {
        RequestsEdit.$buildRaw(change).up.$create().$then(val => {
            humane.success("Up voted");
        });
    }

    function downvote(change) {
        RequestsEdit.$buildRaw(change).down.$create().$then(val => {
            humane.success("Down voted");
        });
    }
}
