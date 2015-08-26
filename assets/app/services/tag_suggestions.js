angular.module("wust.services").service("TagSuggestions", TagSuggestions);

TagSuggestions.$inject = ["Search", "DiscourseNode"];

function TagSuggestions(Search, DiscourseNode) {
    this.search = search;

    let initialSuggestions;

    function search(title) {
        if (_.isEmpty(title)) {
            if (initialSuggestions === undefined)
                initialSuggestions = searchTags("");

            return initialSuggestions;
        } else {
            return searchTags(title);
        }
    }
    function searchTags(title) {
        return Search.$search({
            title: title,
            label: DiscourseNode.TagLike.label,
            size: 8,
            page: 0
        });
    }
}
