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

    function unvote(change) {
        RequestsTag.$buildRaw(change).neutral.$create().$then(val => {
            change.vote = null;
            humane.success("Unvoted");
        });
    }

    function upvote(change) {
        if (change.vote && change.vote.weight > 0)
            return unvote(change);

        RequestsTag.$buildRaw(change).up.$create().$then(val => {
            change.vote = val;
            humane.success("Upvoted");
        });
    }

    function downvote(change) {
        if (change.vote && change.vote.weight < 0)
            return unvote(change);

        RequestsTag.$buildRaw(change).down.$create().$then(val => {
            change.vote = val;
            humane.success("Downvoted");
        });
    }
}
