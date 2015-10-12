angular.module("wust.components").controller("TagDetailsCtrl", TagDetailsCtrl);

TagDetailsCtrl.$inject = ["$stateParams", "Scope", "Search", "DiscourseNode", "StreamService", "Auth", "ContextService"];

function TagDetailsCtrl($stateParams, Scope, Search, DiscourseNode, StreamService, Auth, ContextService) {
    let vm = this;

    let postSize = 30;
    let postPage = 0;

    vm.loadMorePosts = loadMorePosts;
    vm.addTagStream = addTagStream;
    vm.changedInherits = changedInherits;
    vm.auth = Auth;

    vm.tag = Scope.$find($stateParams.id);
    // vm.tag.$then(() => {
    //     ContextService.setContext(vm.tag);
    // });

    //TODO: tags/id/posts should honor inherits relation
    // vm.contributions = vm.tag.posts.$search({
    //     page: postPage,
    //     size: postSize
    // });
    // for now use search api
    vm.contributions = Search.$search({
        label: DiscourseNode.Post.label,
        tagsAll: [$stateParams.id],
        sortOrder: wust.SortOrder().QUALITY,
        page: postPage,
        size: postSize
    });

    function loadMorePosts() {
        if (vm.tag === undefined)
            return false;

        postPage++;
        return vm.contributions.$fetch({
            page: postPage,
            size: postSize
        });
    }

    function addTagStream() {
        if(!vm.tag)
            return;

        StreamService.push({tagsAll: [vm.tag]});
        humane.success(`Added stream for '${vm.tag.title}'`);
    }

    function changedInherits(list, type, tag) {
        switch (type) {
            case "add":
                list.$buildRaw(tag).$save({}).$then(() => vm.contributions.$refresh());
                break;
            case "remove":
                list.$buildRaw(tag).$destroy().$then(() => vm.contributions.$refresh());
                break;
        }
    }
}
