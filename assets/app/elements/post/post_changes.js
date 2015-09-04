angular.module("wust.elements").directive("postTagChanges", postTagChanges);
angular.module("wust.elements").directive("postEditChanges", postEditChanges);

postTagChanges.$inject = [];
function postTagChanges() {
    return {
        restrict: "A",
        templateUrl: "elements/post/post_tag_changes.html",
        scope: {
            postTagChanges: "=",
            onApply: "&"
        },
        controller: postTagChangesCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

postEditChanges.$inject = [];
function postEditChanges() {
    return {
        restrict: "A",
        templateUrl: "elements/post/post_edit_changes.html",
        scope: {
            postEditChanges: "=",
            onApply: "&"
        },
        controller: postEditChangesCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

class VotingEditor {
    constructor(service, onApply = _.noop) {
        this.service = service;
        this.onApply = onApply;
    }

    applyChange(change, response) {
        change.vote = response.vote;
        change.votes = response.votes;
        if (response.node)
            this.onApply({response: response.node});
    }

    unvote(change) {
        this.service.$buildRaw(change).neutral.$create().$then(val => {
            this.applyChange(change, val);
            humane.success("Unvoted");
        });
    }

    up(change) {
        if (change.vote && change.vote.weight > 0)
            return this.unvote(change);

        this.service.$buildRaw(change).up.$create().$then(val => {
            this.applyChange(change, val);
            humane.success("Upvoted");
        });
    }

    down(change) {
        if (change.vote && change.vote.weight < 0)
            return this.unvote(change);

        this.service.$buildRaw(change).down.$create().$then(val => {
            this.applyChange(change, val);
            humane.success("Downvoted");
        });
    }
}

postEditChangesCtrl.$inject = ["RequestsEdit"];
function postEditChangesCtrl(RequestsEdit) {
    let vm = this;

    vm.voting = new VotingEditor(RequestsEdit);
}

postTagChangesCtrl.$inject = ["RequestsTag"];
function postTagChangesCtrl(RequestsTag) {
    let vm = this;

    vm.voting = new VotingEditor(RequestsTag);
}
