angular.module("wust.components").controller("VotesCtrl", VotesCtrl);

VotesCtrl.$inject = ["ChangeRequests"];

function VotesCtrl(ChangeRequests) {
    //TODO: deliver .quality in change.tag, to display the removed tag at the original position in the taglist

    //TODO: WORKAROUND: list of seen change request ids
    //sometimes we skip a request and get new ones but the skipped action did not succeed before and so we get duplicates back.
    let seenIds = [];

    let vm = this;
    let pageSize = 5;
    let refreshWhenLessThan = 3;
    let loadedFullPage = true;

    vm.isLoading = true;
    vm.changes = ChangeRequests.$search({size: pageSize}).$then( () => {
        loadedFullPage = vm.changes.length === pageSize;
        if(vm.changes.length > 0) next();
        vm.isLoading = false;
    } );

    vm.upvote = upvote;
    vm.downvote = downvote;
    vm.skip = skip;
    vm.is = (actiontype) => vm.change.type === actiontype;

    function next() {
        // approved by Felix: die wörter sind richtig.
        do {
            vm.change = vm.changes.shift();
        } while (vm.change !== undefined && _.contains(seenIds, vm.change.id));

        if( vm.change !== undefined ) {
            seenIds.push(vm.change.id);
            vm.showMiddleTab = true;
            vm.actionclasses = {"action": true};
            vm.actionclasses[vm.change.type] = true;
            vm.showDescription = vm.change.oldDescription || vm.change.newDescription || (vm.is("Edit") && vm.description);
        }

        if( loadedFullPage && vm.changes.length < refreshWhenLessThan ) {
            vm.isLoading = true;
            vm.changes.$fetch({skip: vm.changes.length}).$then(response => {
                loadedFullPage = vm.changes.length === pageSize;
                if(vm.change === undefined && vm.changes.length > 0) {
                    next();
                }
                vm.isLoading = false;
            });
        }
    }

    function upvote() {
        ChangeRequests.$buildRaw(_.pick(vm.change, "id")).up.$create().$then(response => {
            // humane.success("Upvoted change request");
        }, resp => humane.error(resp.$response.data));
        next();
    }

    function downvote() {
        ChangeRequests.$buildRaw(_.pick(vm.change, "id")).down.$create().$then(response => {
            // humane.success("Downvoted change request");
        }, resp => humane.error(resp.$response.data));
        next();
    }

    function skip() {
        vm.change.skipped.$create().$then(response => {
            // humane.success("Skipped change request");
        }, resp => humane.error(resp.$response.data));
        next();
    }
}
