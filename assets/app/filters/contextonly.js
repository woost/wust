angular.module("wust.filters").filter("contextonly", contextonly);

contextonly.$inject = ["$filter"];

function contextonly($filter) {
    return function(tags) {
        return _.select(tags, tag => tag.isContext);
    };
}
