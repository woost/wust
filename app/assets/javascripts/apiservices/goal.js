angular.module("wust").factory("Goal", function(restmod) {
    return restmod.model("/goals");
});
