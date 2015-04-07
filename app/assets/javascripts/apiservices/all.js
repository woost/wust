angular.module("wust").factory("All", function(restmod) {
    return restmod.model("/all");
});
