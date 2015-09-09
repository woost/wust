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
            tagType: "@"
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

    function onChangeDistributor(type, tag) {
        if (vm.onChange)
            vm.onChange({type, tag});
    }

    function searchTags(term) {
        if(delayedTriggerSearch)
            clearTimeout(delayedTriggerSearch);

        return $q((resolve, reject) => {
            let label;
            switch (vm.tagType) {
                //TODO: expose labels without own node api in schema object from api
                case "context":
                    // label = DiscourseNode.Scope.label;
                    label = "SCOPE";
                    break;
                case "classification":
                    // label = DiscourseNode.Classification.label;
                    label = "CLASSIFICATION";
                    break;
            }

            delayedTriggerSearch = setTimeout(() => TagSuggestions.search(term, label).$then(resolve, reject), searchTriggerDelay);
        });
    }
}
