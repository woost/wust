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
            alwaysShow: "@"
        },
        controller: editTaglistCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

editTaglistCtrl.$inject = ["TagSuggestions", "$q"];

function editTaglistCtrl(TagSuggestions, $q) {
    let vm = this;

    let searchTriggerDelay = 200;
    let delayedTriggerSearch;

    vm.searchTags = searchTags;
    vm.onChange = vm.onChange || _.noop;

    function searchTags(term) {
        if(delayedTriggerSearch)
            clearTimeout(delayedTriggerSearch);

        return $q((resolve, reject) => {
            delayedTriggerSearch = setTimeout(() => TagSuggestions.search(term).$then(resolve, reject), searchTriggerDelay);
        });
    }
}
