angular.module("wust.components").controller("VotesCtrl", VotesCtrl);

VotesCtrl.$inject = ["InstantRequests"];

function VotesCtrl(InstantRequests) {
    let vm = this;

    let changes = InstantRequests.$search();

    let changeindex = 0;
    changes.$then( () => {
            vm.change = changes[changeindex];
            vm.showundo = false;
    });

    vm.skip = skip;
    vm.undo = undo;

    function skip() {
        changeindex = (changeindex + 1) % changes.length;
        vm.change = changes[changeindex];
        vm.showundo = true;
    }

    function undo() {
        changeindex = (changes.length + changeindex - 1) % changes.length;
        vm.change = changes[changeindex];
    }

}
