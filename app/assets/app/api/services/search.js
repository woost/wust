angular.module("wust.api").factory("Search", function(restmod) {
    return restmod.model("/search");
});
