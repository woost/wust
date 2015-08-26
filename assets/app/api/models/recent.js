angular.module("wust.api").factory("Recent", Recent);

Recent.$inject = ["restmod"];

function Recent(restmod) {
    return restmod.model("/recent");
}
