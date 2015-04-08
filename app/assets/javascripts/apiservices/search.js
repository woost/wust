angular.module("wust").factory("Search", function(restmod) {
    return restmod.model("/search");
});
