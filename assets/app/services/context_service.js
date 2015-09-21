angular.module("wust.services").service("ContextService", ContextService);

ContextService.$inject = ["$rootScope", "Helpers", "User", "Auth"];

function ContextService($rootScope, Helpers, User, Auth) {
    let self = this;

    this.currentContexts = [];
    this.emitChangedEvent = emitChangedEvent;
    this.updateKarma = updateKarma;
    this.setContext = setContext;
    this.contextStyle = {};

    function updateKarma() {
        if( Auth.current.userId ) {
            User.$buildRaw({id: Auth.current.userId}).karma.$search().$then(response =>
                    response.forEach(k => {
                        let context =  _.find(this.currentContexts, c => c.id === k.id);
                        if( context )
                            context.karma = k.karma;
                    }));
        }
    }

    function setContext(node) {
        let firstContext = Helpers.sortByIdQuality(node.tags)[0];
        if (firstContext && (this.currentContexts.length !== 1 || this.currentContexts.length === 1 && this.currentContexts[0].id !== firstContext.id)) {
            this.currentContexts.length = 0;
            this.currentContexts.push(firstContext);
            this.emitChangedEvent();
        }
    }

    function emitChangedEvent() {
        this.updateKarma();
        this.contextStyle["background-color"] = this.currentContexts.length > 0 ? Helpers.navBackgroundColor(this.currentContexts[0]) : undefined;
        this.contextStyle["border-bottom"] = this.currentContexts.length > 0 ? ("1px solid " + Helpers.contextCircleBorderColor(this.currentContexts[0])) : undefined;
        $rootScope.$emit("context.changed");
    }
}

