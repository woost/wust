angular.module("wust.services").service("KarmaService", KarmaService);

KarmaService.$inject = ["User", "Auth", "LiveService", "$rootScope"];

function KarmaService(User, Auth, LiveService, $rootScope) {
    let self = this;

    this.refreshKarma = refreshKarma;
    this.karmaInContext = karmaInContext;
    this.voteWeightInContexts = voteWeightInContexts;
    this.karma = {
        contexts: [],
        sum: 0
    };

    refreshKarma();
    registerEvent();

    function karmaInContext(context) {
        let found = _.find(self.karma.contexts, t => t.id === context.id);
        return found ? found.karma : 0;
    }

    function voteWeightInContexts(contexts) {
        let sum = contexts.map(context => karmaInContext(context)).reduce((a,b) => a+b, 0);
        return wust.Moderation().voteWeight(sum);
    }

    function registerEvent() {
        if ( Auth.current.userId ) {
            LiveService.registerUser(Auth.current.userId, event => {
                switch (event.kind) {
                    case "karmalog":
                        let contexts = _.uniq(_.flatten(event.data.map(log => {
                            return log.contexts.map(c => {
                                let exist = _.find(self.karma.contexts, {id: c.id}) || c;
                                if (exist.karma === undefined)
                                    exist.karma = 0;

                                exist.karma += log.karmaChange;
                                return exist;
                            });
                        })).concat(self.karma.contexts), "id");
                        updateStats(contexts);
                        break;
                }
            });
        }
    }

    function updateStats(contexts) {
        self.karma.contexts = contexts;
        self.karma.sum = contexts.length ? _.map(contexts, r => r.karma).reduce((a,b) => a+b) : 0;
        $rootScope.$apply(() => $rootScope.$broadcast("karma.changed"));
    }

    function refreshKarma() {
        if ( Auth.current.userId ) {
            User.$buildRaw({id: Auth.current.userId}).karmaContexts.$search().$then(response => {
                updateStats(response);
            });
        }
    }
}

