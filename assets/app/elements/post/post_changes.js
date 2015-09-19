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
    constructor(vm, prop, service, onApply = _.noop) {
        this.vm = vm;
        this.prop = prop;
        this.service = service;
        this.onApply = onApply;
    }

    get list() {
        return this.vm[this.prop];
    }

    applyChange(change, response) {
        change.vote = response.vote;
        change.votes = response.votes;
        change.applied = response.applied;
        if (change.applied !== 0) {
            _.remove(this.list, {id:change.id});
        }

        if (change.applied === -1) {
            humane.success("Change request rejected");
        }
        if (change.applied > 0) {
            this.onApply({node: response.node, tag: change.tag, isRemove: change.isRemove});
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

    vm.voting = new VotingEditor(vm, "postEditChanges", RequestsEdit, vm.onApply);
}

postTagChangesCtrl.$inject = ["RequestsTags"];
function postTagChangesCtrl(RequestsTags) {
    let vm = this;

    vm.voting = new VotingEditor(vm, "postTagChanges", RequestsTags, vm.onApply);
}
