angular.module("wust.filters").filter("hideHidden", hideHidden);

hideHidden.$inject = [];

function hideHidden() {
    return function(xs) {
        return _.reject(xs, "_hidden");
    };
}
