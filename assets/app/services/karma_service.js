angular.module("wust.services").service("KarmaService", KarmaService);

KarmaService.$inject = ["User", "Auth"];

function KarmaService(User, Auth) {
    let self = this;

    let listeners = [];

    this.onChange = (handler) => listeners.push(handler);
    this.refreshKarma = refreshKarma;
    this.karma = {
        tags: [],
        sum: 0
    };

    refreshKarma();

    function refreshKarma() {
        if ( Auth.current.userId ) {
            User.$buildRaw({id: Auth.current.userId}).karma.$search().$then(response => {
                self.karma.tags = response;
                self.karma.sum = response.length ? _.map(response, r => r.karma).reduce((a,b) => a+b) : 0;
                listeners.forEach(listener => listener(self.karma));
            });
        }
    }
}

