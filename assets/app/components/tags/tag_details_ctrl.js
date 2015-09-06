angular.module("wust.components").controller("TagDetailsCtrl", TagDetailsCtrl);

TagDetailsCtrl.$inject = ["$stateParams", "TagLike", "Search", "DiscourseNode", "StreamService"];

function TagDetailsCtrl($stateParams, TagLike, Search, DiscourseNode, StreamService) {
    let vm = this;

    let postSize = 30;
    let postPage = 0;

    vm.loadMorePosts = loadMorePosts;
    vm.addTagStream = addTagStream;

    vm.tag = TagLike.$find($stateParams.id);
    //TODO: tags/id/posts should honor inherits relation
    // vm.contributions = vm.tag.posts.$search({
    //     page: postPage,
    //     size: postSize
    // });
    // for now use search api
    vm.contributions = Search.$search({
        label: DiscourseNode.Post.label,
        tags: [$stateParams.id],
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

        StreamService.push([vm.tag]);
        humane.success(`Added stream for '${vm.tag.title}'`);
    }
}
