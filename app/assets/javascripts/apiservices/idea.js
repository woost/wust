angular.module("wust").factory("Idea", function(restmod) {
    return restmod.model("/ideas");
});
