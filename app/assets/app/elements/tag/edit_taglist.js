angular.module("wust.elements").directive("editTaglist", editTaglist);

editTaglist.$inject = [];

function editTaglist() {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/tag/edit_taglist.html",
        scope: {
            node: "="
        },
        controller: editTaglistCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

editTaglistCtrl.$inject = ["Search", "DiscourseNode"];

// like edit_post: expects the node to be a session
function editTaglistCtrl(Search, DiscourseNode) {
    let vm = this;

    vm.nodeInfo = DiscourseNode.TagLike;

    vm.searchTags = searchTags;

    function searchTags(title) {
        return Search.$search({
            title: title,
            label: DiscourseNode.TagLike.label
        });
    }
}
