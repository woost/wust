angular.module("wust.api").factory("Search", Search);

Search.$inject = ["restmod"];

function Search(restmod) {
    return restmod.model("/search");
}
