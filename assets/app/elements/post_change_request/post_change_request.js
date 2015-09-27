angular.module("wust.elements").directive("postChangeRequest", postChangeRequest);

postChangeRequest.$inject = [];

function postChangeRequest() {
    return {
        restrict: "E",
        templateUrl: "elements/post_change_request/post_change_request.html",
        transclude: true,
        scope: {
            changes: "=",
            onApply: "&",
            onTagApply: "&",
            onDeleteApply: "&"
        },
        controller: postChangeRequestCtrl,
        controllerAs: "vm",
        bindToController: true,
    };
}


postChangeRequestCtrl.$inject = ["ChangeRequests"];

function postChangeRequestCtrl(ChangeRequests) {
    let vm = this;

    class VotingEditor {
        constructor() {
        }

        applyChange(change, response) {
            change.vote = response.vote;
            change.votes = response.votes;
            change.status = response.status;

            if (change.status !== 0) {
                _.remove(vm.changes, {id:change.id});
            }

            if (change.status === -1) {
                humane.success("Change request rejected");
            } else if (change.status > 0) {
                humane.success("Change request accepted");
                if (change.type === "Edit")
                    vm.onApply({node: response.node});
                else if (change.type === "Delete")
                    vm.onDeleteApply();
                else
                    vm.onTagApply({tag: change.tag, isRemove: change.isRemove});
            }
        }

        unvote(change) {
            ChangeRequests.$buildRaw(change).neutral.$create().$then(val => {
                this.applyChange(change, val);
            }, response => {
                _.remove(vm.changes, {id:change.id});
                humane.error(response.$response.data);
            });
        }

        up(change) {
            if (change.vote && change.vote.weight > 0)
                return this.unvote(change);

            ChangeRequests.$buildRaw(change).up.$create().$then(val => {
                this.applyChange(change, val);
            }, response => {
                _.remove(vm.changes, {id:change.id});
                humane.error(response.$response.data);
            });
        }

        down(change) {
            if (change.vote && change.vote.weight < 0)
                return this.unvote(change);

            ChangeRequests.$buildRaw(change).down.$create().$then(val => {
                this.applyChange(change, val);
            }, response => {
                _.remove(vm.changes, {id:change.id});
                humane.error(response.$response.data);
            });
        }
    }

    vm.voting = new VotingEditor(vm);
}

