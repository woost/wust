angular.module("wust.api").factory("User", function(restmod) {
    return restmod.model("/users");
});
