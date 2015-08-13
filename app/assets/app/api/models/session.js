angular.module("wust.api").service("Session", Session);

Session.$inject = ["restmod"];

function Session(restmod) {
    let self = this;

    this.votes = restmod.singleton("/session/votes");
    this.update = update;
    this.forget = forget;

    function update() {
        self.votes.$fetch();
    }

    function forget() {
        self.votes.$clear();
    }
}
