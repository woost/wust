angular.module("wust").factory("Problem", function(restmod) {
    return restmod.model("/problems");
});
