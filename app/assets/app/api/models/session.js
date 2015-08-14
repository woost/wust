angular.module("wust.api").service("Session", Session);

Session.$inject = ["restmod"];

function Session(restmod) {
    let self = this;

    this.votes = [];
    this.update = update;
    this.forget = forget;
    this.addVote = addVote;
    this.getVote = getVote;

    function getVote(startId, endId) {
        return _.find(self.votes, {
            startId, endId
        });
    }

    function addVote(startId, endId, vote) {
        let existing = getVote(startId, endId);
        if (existing === undefined) {
            existing = {
                startId, endId
            };
            self.votes.push(existing);
        }

        existing.weight = vote.weight;
    }

    function update() {
        restmod.singleton("/session/votes").$fetch().$then(val => {
            self.votes = _.values(val.$encode());
        });
    }

    function forget() {
        self.votes = [];
    }
}