angular.module("wust.components").controller("TagsCtrl", TagsCtrl);

TagsCtrl.$inject = ["$stateParams", "TagLike", "StreamService"];

function TagsCtrl($stateParams, TagLike, StreamService) {
    let vm = this;

    let tagSize = 30;
    let tagPage = 0;

    vm.loadMoreTags = loadMoreTags;

    vm.tags = TagLike.$search({
        page: tagPage,
        size: tagSize
    });

    function loadMoreTags() {
        tagPage++;
        return vm.tags.$fetch({
            page: tagPage,
            size: tagSize
        });
    }
}
