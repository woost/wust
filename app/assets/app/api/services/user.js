angular.module("wust.api").factory("User", User);

User.$inject = ["restmod"];

function User(restmod) {
    return restmod.model("/users");
}
