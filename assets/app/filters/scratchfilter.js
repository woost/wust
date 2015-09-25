angular.module("wust.filters").filter("scratchfilter", scratchfilter);

scratchfilter.$inject = ["$filter"];

function scratchfilter($filter) {
    return function(value, showEdits) {
        return _.select(value, post => showEdits && !post.visible && !post.isPristine || !showEdits && post.visible);
    };
}
