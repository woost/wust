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

    vm.nodeInfo = DiscourseNode.Tag;

    vm.searchTags = searchTags;
    vm.onChange = vm.onChange || _.noop;
    vm.selectTag = selectTag;
    vm.deselectTag = deselectTag;

    function selectTag(maybeTags) {
        let tag = _.isArray(maybeTags) ? maybeTags[0] : maybeTags;
        if (!maybeTags)
            return;

        vm.tags.push(tag);
        vm.onChange();
    }

    function deselectTag(tag) {
        _.remove(vm.tags, tag);
        vm.onChange();
    }

    function searchTags(title) {
        return Search.$search({
            title: title,
            label: DiscourseNode.Tag.label
        });
    }
}
