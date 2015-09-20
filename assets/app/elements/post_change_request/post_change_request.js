angular.module("wust.elements").directive("postChangeRequest", postChangeRequest);

postChangeRequest.$inject = [];

function postChangeRequest() {
    return {
        restrict: "E",
        templateUrl: "elements/post_change_request/post_change_request.html",
        transclude: true,
        scope: {
            changes: "=",
            template: "@",
            service: "=",
            onApply: "&",
        },
        controller: postChangeRequestCtrl,
        controllerAs: "vm",
        bindToController: true,
    };
}

class VotingEditor {
    constructor(vm, service, onApply = _.noop) {
        this.vm = vm;
    }

    get list() {
        return this.vm.changes;
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
            this.vm.onApply({node: response.node, tag: change.tag, isRemove: change.isRemove});
        }
    }

    unvote(change) {
        this.vm.service.$buildRaw(change).neutral.$create().$then(val => {
            this.applyChange(change, val);
            humane.success("Unvoted");
        }, response => humane.error(response.$response.data));
    }

    up(change) {
        if (change.vote && change.vote.weight > 0)
            return this.unvote(change);

        this.vm.service.$buildRaw(change).up.$create().$then(val => {
            this.applyChange(change, val);
        }, response => humane.error(response.$response.data));
    }

    down(change) {
        if (change.vote && change.vote.weight < 0)
            return this.unvote(change);

        this.vm.service.$buildRaw(change).down.$create().$then(val => {
            this.applyChange(change, val);
        }, response => humane.error(response.$response.data));
    }
}

postChangeRequestCtrl.$inject = [];

function postChangeRequestCtrl() {
    let vm = this;

    vm.voting = new VotingEditor(vm);
}

