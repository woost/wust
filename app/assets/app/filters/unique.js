angular.module("wust.filters").filter("unique", unique);

unique.$inject = [];

function unique() {
    return (arr, field) => _.uniq(arr, field);
}
