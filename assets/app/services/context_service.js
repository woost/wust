angular.module("wust.services").service("ContextService", ContextService);

ContextService.$inject = ["$rootScope"];

function ContextService($rootScope) {

    this.currentContexts = [];

    this.emitChangedEvent = emitChangedEvent;

    function emitChangedEvent() {
        $rootScope.$emit("context.changed");
    }
}

