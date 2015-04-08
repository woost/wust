angular.module("wust").factory("Graph", function(restmod) {
    return restmod.singleton("/graphs");
});
