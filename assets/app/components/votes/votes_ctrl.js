angular.module("wust.components").controller("VotesCtrl", VotesCtrl);

VotesCtrl.$inject = ["InstantRequests"];

function VotesCtrl(InstantRequests) {
    let vm = this;

    let changes = InstantRequests.$search();

    let changeindex = -1;
    changes.$then( () => {
            vm.next();
            vm.showundo = false;
    });

    vm.next = next;
    vm.undo = undo;
    vm.is = (actiontype) => vm.change.type === actiontype;

    function next() {
        changeindex = (changeindex + 1) % changes.length;
        vm.change = changes[changeindex];
        vm.showundo = true;
        vm.showMiddleTab = true;
        vm.actionclasses = {"action": true};
        vm.actionclasses[vm.change.type] = true;
        vm.showDescription = vm.change.oldDescription || vm.change.newDescription || (vm.is("Edit") && vm.description);

    }

    function undo() {
        changeindex = (changes.length + changeindex - 1) % changes.length;
        vm.change = changes[changeindex];
    }

}
