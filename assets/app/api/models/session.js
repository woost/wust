angular.module("wust.api").service("Session", Session);

Session.$inject = ["restmod"];

function Session(restmod) {
    this.history = restmod.singleton("/session/history");
}
