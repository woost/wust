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

    function unvote(change) {
        RequestsEdit.$buildRaw(change).neutral.$create().$then(val => {
            change.vote = null;
            humane.success("Unvoted");
        });
    }

    function upvote(change) {
        if (change.vote && change.vote.weight > 0)
            return unvote(change);

        RequestsEdit.$buildRaw(change).up.$create().$then(val => {
            change.vote = val;
            humane.success("Upvoted");
        });
    }

    function downvote(change) {
        if (change.vote && change.vote.weight < 0)
            return unvote(change);

        RequestsEdit.$buildRaw(change).down.$create().$then(val => {
            change.vote = val;
            humane.success("Downvoted");
        });
    }
}
