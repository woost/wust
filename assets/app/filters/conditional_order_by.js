angular.module("wust.filters").filter("conditionalOrderBy", conditionalOrderBy);

conditionalOrderBy.$inject = ["$filter"];

function conditionalOrderBy($filter) {
    return function(xs, expression, reverse) {
        if (expression === undefined) return xs;
        return $filter("orderBy")(xs, expression, reverse);
    };
}
