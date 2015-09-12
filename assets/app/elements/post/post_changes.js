angular.module("wust.elements").directive("postTagChanges", postTagChanges);
angular.module("wust.elements").directive("postEditChanges", postEditChanges);

postTagChanges.$inject = [];
function postTagChanges() {
    return {
        restrict: "A",
        templateUrl: "elements/post/post_tag_changes.html",
        scope: {
            postTagChanges: "=",
            onApply: "&",
            remove: "@"
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
    constructor(list, service, onApply = _.noop) {
        this.list = list;
        this.service = service;
        this.onApply = onApply;
    }

    applyChange(change, response) {
        change.vote = response.vote;
        change.votes = response.votes;
        if (response.node) {
            _.remove(this.list, change);
            this.onApply({response: response.node, tag: change.tags ? change.tags[0] : undefined});
        }
    }

    unvote(change) {
        this.service.$buildRaw(change).neutral.$create().$then(val => {
            this.applyChange(change, val);
            humane.success("Unvoted");
        }, response => humane.error(response.$response.data));
    }

    up(change) {
        if (change.vote && change.vote.weight > 0)
            return this.unvote(change);

        this.service.$buildRaw(change).up.$create().$then(val => {
            this.applyChange(change, val);
            humane.success("Upvoted");
        }, response => humane.error(response.$response.data));
    }

    down(change) {
        if (change.vote && change.vote.weight < 0)
            return this.unvote(change);

        this.service.$buildRaw(change).down.$create().$then(val => {
            this.applyChange(change, val);
            humane.success("Downvoted");
        }, response => humane.error(response.$response.data));
    }
}

postEditChangesCtrl.$inject = ["RequestsEdit"];
function postEditChangesCtrl(RequestsEdit) {
    let vm = this;

    vm.voting = new VotingEditor(vm.postEditChanges, RequestsEdit, vm.onApply);
}

postTagChangesCtrl.$inject = ["RequestsTags"];
function postTagChangesCtrl(RequestsTags) {
    let vm = this;

    vm.voting = new VotingEditor(vm.postTagChanges, RequestsTags, vm.onApply);
}
