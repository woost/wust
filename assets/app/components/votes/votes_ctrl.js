angular.module("wust.components").controller("VotesCtrl", VotesCtrl);

VotesCtrl.$inject = ["InstantRequests", "RequestsTags", "RequestsEdit", "RequestsDelete"];

function VotesCtrl(InstantRequests, RequestsTags, RequestsEdit, RequestsDelete) {
    let vm = this;

    let changes = InstantRequests.$search();

    let changeindex = -1;
    changes.$then( () => {
            vm.next();
            vm.showundo = false;
    });

    let serviceMap = {
        Delete: RequestsDelete,
        AddTag: RequestsTags,
        RemoveTag: RequestsTags,
        Edit: RequestsEdit
    };

    //TODO: deliver .quality in change.tag, to display the removed tag at the original position in the taglist

    vm.upvote = upvote;
    vm.downvote = downvote;
    vm.next = next;
    vm.is = (actiontype) => vm.change.type === actiontype;

    function upvote() {
        let service = serviceMap[vm.change.type];
        service.$buildRaw(_.pick(vm.change, "id")).up.$create().$then(response => {
            humane.success("Upvoted change request");
            changes.splice(changeindex, 1);
            changeindex -= 1;
            next();
        });
    }

    function downvote() {
        let service = serviceMap[vm.change.type];
        service.$buildRaw(_.pick(vm.change, "id")).down.$create().$then(response => {
            humane.success("Downvoted change request");
            changes.splice(changeindex, 1);
            changeindex -= 1;
            next();
        });
    }

    function next() {
        if (changes.length > 0) {
            changeindex = (changeindex + 1) % changes.length;
            vm.change = changes[changeindex];
            vm.showundo = true;
            vm.showMiddleTab = true;
            vm.actionclasses = {"action": true};
            vm.actionclasses[vm.change.type] = true;
            vm.showDescription = vm.change.oldDescription || vm.change.newDescription || (vm.is("Edit") && vm.description);
        } else {
            vm.change = undefined;
        }
    }
}
