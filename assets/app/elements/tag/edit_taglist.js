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
            embedSuggestions: "@",
            emptySearchOnlyFirst: "="
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
    let tagLabels;
    switch (vm.tagType) {
        //TODO: expose labels without own node api in schema object from api
        case "classification":
            // tagLabel = DiscourseNode.Classification.label;
            tagLabels = ["CLASSIFICATION"];
            vm.placeholder = vm.placeholder || "Add Classification";
        break;
        case "taglike":
            // tagLabel = DiscourseNode.Classification.label;
            tagLabels = ["CLASSIFICATION", "SCOPE"];
            vm.placeholder = vm.placeholder || "Add Tag";
        break;
        case "context":
            /* falls through */
        default:
            // tagLabel = DiscourseNode.Scope.label;
            tagLabels = ["SCOPE"];
            vm.placeholder = vm.placeholder || "Add Context";
        break;
    }


    function onChangeDistributor(type, tag) {
        // set isContext for contexts, as the some styles for local tags depend on it
        if (type === "add" && tag.isContext === undefined && tag.id === undefined)
            tag.isContext = true;

        if (vm.onChange)
            vm.onChange({type, tag});
    }

    function searchTags(term) {
        if(delayedTriggerSearch)
            clearTimeout(delayedTriggerSearch);

        let chosenLabels = vm.emptySearchOnlyFirst && term === "" ? [tagLabels[0]] : tagLabels;
        return $q((resolve, reject) => {
            delayedTriggerSearch = setTimeout(() => $q.all(chosenLabels.map(tagLabel => {
                return TagSuggestions.search(term, tagLabel);
            })).then(resolve, reject), searchTriggerDelay);
        });
    }
}
