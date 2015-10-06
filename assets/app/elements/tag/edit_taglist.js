angular.module("wust.elements").directive("editTaglist", editTaglist);

editTaglist.$inject = [];

function editTaglist() {
    return {
        restrict: "A",
        templateUrl: "elements/tag/edit_taglist.html",
        scope: {
            tags: "=",
            setFocus: "=",
            onChange: "&",
            existingOnly: "@",
            alwaysShow: "@",
            emptyShow: "@",
            tagType: "@",
            editClassification: "@",
            placeholder: "@",
            embedSuggestions: "@"
        },
        controller: editTaglistCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

editTaglistCtrl.$inject = ["TagSuggestions", "DiscourseNode", "$q"];

function editTaglistCtrl(TagSuggestions, DiscourseNode, $q) {
    let vm = this;

    let searchTriggerDelay = 200;
    let delayedTriggerSearch;

    vm.onChangeDistributor = onChangeDistributor;
    vm.searchTags = searchTags;
    switch (vm.tagType) {
        //TODO: expose labels without own node api in schema object from api
        case "classification":
            // tagLabel = DiscourseNode.Classification.label;
            vm.tagLabel = "CLASSIFICATION";
            vm.placeholder = vm.placeholder || "Add Classification";
        break;
        case "taglike":
            // tagLabel = DiscourseNode.Classification.label;
            vm.tagLabel = "TAGLIKE";
            vm.placeholder = vm.placeholder || "Add Tag";
        break;
        case "context":
            /* falls through */
        default:
            // tagLabel = DiscourseNode.Scope.label;
            vm.tagLabel = "SCOPE";
            vm.placeholder = vm.placeholder || "Add Context";
        break;
    }


    function onChangeDistributor(type, tag) {
        // set isContext for contexts, as the some styles for local tags depend on it
        if (type === "add" && tag.isContext === undefined)
            tag.isContext = vm.tagLabel === "SCOPE";

        if (vm.onChange)
            vm.onChange({type, tag});
    }

    function searchTags(term, tagLabel) {
        if(delayedTriggerSearch)
            clearTimeout(delayedTriggerSearch);

        return $q((resolve, reject) => {
            delayedTriggerSearch = setTimeout(() => TagSuggestions.search(term, tagLabel).$then(resolve, reject), searchTriggerDelay);
        });
    }
}
