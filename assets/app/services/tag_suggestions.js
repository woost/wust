angular.module("wust.services").service("TagSuggestions", TagSuggestions);

TagSuggestions.$inject = ["Search", "DiscourseNode"];

function TagSuggestions(Search, DiscourseNode) {
    this.search = search;

    let initialSuggestions = {
    };
    let initialContextSuggestions;

    function search(title, label = DiscourseNode.TagLike.label) {
        if (_.isEmpty(title)) {
            if (initialSuggestions[label] === undefined)
                initialSuggestions[label] = searchTags("", label);

            return initialSuggestions[label];
        } else {
            return searchTags(title, label);
        }
    }
    function searchTags(title, label) {
        return Search.$search({
            title: title,
            label: label,
            size: 8,
            page: 0
        });
    }
}
