angular.module("wust.components").controller("VotesCtrl", VotesCtrl);

VotesCtrl.$inject = ["InstantRequests", "RequestsTags", "RequestsEdit", "RequestsDelete"];

function VotesCtrl(InstantRequests, RequestsTags, RequestsEdit, RequestsDelete) {
    //TODO: voting undo?
    //TODO: deliver .quality in change.tag, to display the removed tag at the original position in the taglist


    let vm = this;
    let pagesize = 5;
    let loadedFullPage = true;

    vm.changes = InstantRequests.$search({size: pagesize}).$then( () => {
        loadedFullPage = vm.changes.length === pagesize;
        if(vm.changes.length > 0) next();
    } );

    let serviceMap = {
        Delete: RequestsDelete,
        AddTag: RequestsTags,
        RemoveTag: RequestsTags,
        Edit: RequestsEdit
    };

    vm.upvote = upvote;
    vm.downvote = downvote;
    vm.skip = skip;
    vm.is = (actiontype) => vm.change.type === actiontype;

    function next() {
        vm.change = vm.changes.shift();

        if( vm.change !== undefined ) {
            vm.showMiddleTab = true;
            vm.actionclasses = {"action": true};
            vm.actionclasses[vm.change.type] = true;
            vm.showDescription = vm.change.oldDescription || vm.change.newDescription || (vm.is("Edit") && vm.description);
        }

        if( loadedFullPage && vm.changes.length < 3 ) {
            vm.changes.$refresh().$then(response => {
                loadedFullPage = vm.changes.length === pagesize;
                if(vm.change === undefined && vm.changes.length > 0) {
                    next();
                }
            });
        }
    }

    function upvote() {
        let service = serviceMap[vm.change.type];
        service.$buildRaw(_.pick(vm.change, "id")).up.$create().$then(response => {
            humane.success("Upvoted change request");
        }, resp => humane.error(resp.$response.data));
        next();
    }

    function downvote() {
        let service = serviceMap[vm.change.type];
        service.$buildRaw(_.pick(vm.change, "id")).down.$create().$then(response => {
            humane.success("Downvoted change request");
        }, resp => humane.error(resp.$response.data));
        next();
    }

    function skip() {
        vm.change.skipped.$create().$then(response => {
            humane.success("Skipped change request");
        }, resp => humane.error(resp.$response.data));
        next();
    }
}
