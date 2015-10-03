angular.module("wust.services").service("KarmaService", KarmaService);

KarmaService.$inject = ["User", "Auth", "$rootScope"];

function KarmaService(User, Auth, $rootScope) {
    let self = this;

    this.refreshKarma = refreshKarma;
    this.karmaInContext = karmaInContext;
    this.voteWeightInContexts = voteWeightInContexts;
    this.karma = {
        contexts: [],
        sum: 0
    };

    refreshKarma();

    function karmaInContext(context) {
        let found = _.find(self.karma.contexts, t => t.id === context.id);
        return found ? found.karma : 0;
    }

    function voteWeightInContexts(contexts) {
        let sum = contexts.map(context => karmaInContext(context)).reduce((a,b) => a+b, 0);
        return wust.Moderation().voteWeight(sum);
    }

    function refreshKarma() {
        if ( Auth.current.userId ) {
            User.$buildRaw({id: Auth.current.userId}).karmaContexts.$search().$then(response => {
                self.karma.contexts = response;
                self.karma.sum = response.length ? _.map(response, r => r.karma).reduce((a,b) => a+b) : 0;
                $rootScope.$broadcast("karma.changed");
            });
        }
    }
}

