angular.module("wust.services").service("KarmaService", KarmaService);

KarmaService.$inject = ["User", "Auth", "$rootScope"];

function KarmaService(User, Auth, $rootScope) {
    let self = this;

    this.refreshKarma = refreshKarma;
    this.karmaInContext = karmaInContext;
    this.karma = {
        tags: [],
        sum: 0
    };

    refreshKarma();

    function karmaInContext(context) {
        let found = _.find(self.karma.tags, t => t.id === context.id);
        return found ? found.karma : 0;
    }

    function refreshKarma() {
        if ( Auth.current.userId ) {
            User.$buildRaw({id: Auth.current.userId}).karmaContexts.$search().$then(response => {
                self.karma.tags = response;
                self.karma.sum = response.length ? _.map(response, r => r.karma).reduce((a,b) => a+b) : 0;
                $rootScope.$broadcast("karma.changed");
            });
        }
    }
}

