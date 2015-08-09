angular.module("wust.components").controller("TagDetailsCtrl", TagDetailsCtrl);

TagDetailsCtrl.$inject = ["$stateParams", "TagLike", "StreamService"];

function TagDetailsCtrl($stateParams, TagLike, StreamService) {
    let vm = this;

    let postSize = 20;
    let postPage = 0;

    vm.loadMorePosts = loadMorePosts;
    vm.addTagStream = addTagStream;

    vm.tag = TagLike.$find($stateParams.id);
    vm.contributions = vm.tag.posts.$search({
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
