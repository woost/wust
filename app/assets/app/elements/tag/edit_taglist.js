angular.module("wust.elements").directive("editTaglist", editTaglist);

editTaglist.$inject = [];

function editTaglist() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/tag/edit_taglist.html",
        scope: {
            tags: "=",
            setFocus: "=",
            onChange: "&",
            existingOnly: "@",
            alwaysShow: "@"
        },
        controller: editTaglistCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

editTaglistCtrl.$inject = ["Search", "DiscourseNode"];

function editTaglistCtrl(Search, DiscourseNode) {
    let vm = this;

    vm.searchTags = searchTags;
    vm.onChange = vm.onChange || _.noop;

    function searchTags(title) {
        return Search.$search({
            title: title,
            label: DiscourseNode.TagLike.label,
            size: 8,
            page: 0
        });
    }
}
