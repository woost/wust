angular.module("wust.services").service("KarmaService", KarmaService);

KarmaService.$inject = ["User", "Auth", "$rootScope"];

function KarmaService(User, Auth, $rootScope) {
    let self = this;

    this.refreshKarma = refreshKarma;
    this.updateKarma = updateKarma;
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

    function updateKarma(data) {
        let contexts = _.uniq(_.flatten(data.map(log => {
            return log.contexts.map(c => {
                let exist = _.find(self.karma.contexts, {id: c.id}) || c;
                if (exist.karma === undefined)
                    exist.karma = 0;

                exist.karma += log.karmaChange;
                return exist;
            });
        })).concat(self.karma.contexts), "id");

        updateStats(contexts);
    }

    function updateStats(contexts) {
        self.karma.contexts = contexts;
        self.karma.sum = contexts.length ? _.map(contexts, r => r.karma).reduce((a,b) => a+b) : 0;
        $rootScope.$broadcast("karma.changed");
    }

    function refreshKarma() {
        if ( Auth.current.userId ) {
            User.$buildRaw({id: Auth.current.userId}).karmaContexts.$search().$then(response => {
                updateStats(response);
            });
        }
    }
}

