angular.module("wust.elements").directive("editStaticTaglist", editStaticTaglist);

editStaticTaglist.$inject = [];

function editStaticTaglist() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/tag/edit_static_taglist.html",
        scope: {
            tags: "=",
            onChange: "&"
        },
        controller: editStaticTaglistCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

editStaticTaglistCtrl.$inject = ["Search", "DiscourseNode"];

function editStaticTaglistCtrl(Search, DiscourseNode) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.TagLike;

    vm.searchTags = searchTags;
    vm.onChange = vm.onChange || _.noop;

    function searchTags(title) {
        return Search.$search({
            title: title,
            label: DiscourseNode.TagLike.label
        });
    }
}
