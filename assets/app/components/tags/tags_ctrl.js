angular.module("wust.components").controller("TagsCtrl", TagsCtrl);

TagsCtrl.$inject = ["$stateParams", "Scope", "StreamService"];

function TagsCtrl($stateParams, Scope, StreamService) {
    let vm = this;

    let tagSize = 30;
    let tagPage = 0;

    vm.loadMoreTags = loadMoreTags;

    vm.tags = Scope.$search({
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
