angular.module("wust.services").service("TagSuggestions", TagSuggestions);

TagSuggestions.$inject = ["Search", "DiscourseNode", "$q", "Helpers"];

function TagSuggestions(Search, DiscourseNode, $q, Helpers) {
    this.search = search;

    let initialSuggestions = {};

    let initialContextSuggestions;

    function search(title, label = DiscourseNode.Scope.label) {
        if (label === "CLASSIFICATION") {
            if (initialSuggestions[label] === undefined)
                initialSuggestions[label] = searchTags("", label);

            let deferred = $q.defer();
            let regex = new RegExp(title, "i");
            initialSuggestions[label].then(suggestions => deferred.resolve(Helpers.sortByIdQuality(suggestions.filter(s => s.title.match(regex)))));
            return deferred.promise;
        } else {
            if (_.isEmpty(title)) {
                if (initialSuggestions[label] === undefined)
                    initialSuggestions[label] = searchTags("", label);

                return initialSuggestions[label];
            } else {
                return searchTags(title, label);
            }
        }
    }
    function searchTags(title, label) {
        return Search.$search({
            term: title,
            label: label,
            size: label === "CLASSIFICATION" ? 100 : 8,
            page: 0
        }).$asPromise();
    }
}
