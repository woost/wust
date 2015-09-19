angular.module("wust.services").service("ContextService", ContextService);

ContextService.$inject = ["$rootScope", "Helpers"];

function ContextService($rootScope, Helpers) {
    let self = this;

    this.currentContexts = [];
    this.emitChangedEvent = emitChangedEvent;
    this.setContext = setContext;
    this.contextStyle = {};

    function setContext(node) {
        let firstContext = Helpers.sortByIdQuality(node.tags)[0];
        if (firstContext && (this.currentContexts.length !== 1 || this.currentContexts.length === 1 && this.currentContexts[0].id !== firstContext.id)) {
            this.currentContexts.length = 0;
            this.currentContexts.push(firstContext);
            this.emitChangedEvent();
        }
    }

    function emitChangedEvent() {
        this.contextStyle["background-color"] = this.currentContexts.length > 0 ? Helpers.navBackgroundColor(this.currentContexts[0]) : undefined;
        this.contextStyle["border-bottom"] = this.currentContexts.length > 0 ? ("1px solid " + Helpers.contextCircleBorderColor(this.currentContexts[0])) : undefined;
        $rootScope.$emit("context.changed");
    }
}

