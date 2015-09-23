angular.module("wust.services").service("ContextService", ContextService);

ContextService.$inject = ["$rootScope", "Helpers", "KarmaService"];

function ContextService($rootScope, Helpers, KarmaService) {
    let self = this;

    this.currentContexts = [];
    this.emitChangedEvent = emitChangedEvent;
    this.setContext = setContext;
    this.setNodeContext = setNodeContext;
    this.contextStyle = {};

    KarmaService.onChange(updateKarma);

    function updateKarma() {
        self.currentContexts.forEach(context => {
            context.karma = KarmaService.karmaInContext(context);
        });
    }

    function setContext(context) {
        this.currentContexts.length = 0;
        this.currentContexts.push(context);
        updateKarma();
        this.emitChangedEvent();
    }

    function setNodeContext(node) {
        let firstContext = Helpers.sortByIdQuality(node.tags)[0];
        if (firstContext && (this.currentContexts.length !== 1 || this.currentContexts.length === 1 && this.currentContexts[0].id !== firstContext.id)) {
            this.setContext(firstContext);
        }
    }

    function emitChangedEvent() {

        this.contextStyle["background-color"] = this.currentContexts.length > 0 ? Helpers.navBackgroundColor(this.currentContexts[0]) : undefined;
        this.contextStyle["border-bottom"] = this.currentContexts.length > 0 ? ("1px solid " + Helpers.contextCircleBorderColor(this.currentContexts[0])) : undefined;
        $rootScope.$emit("context.changed");
    }
}

