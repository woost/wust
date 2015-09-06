angular.module("wust.services").service("ContextService", ContextService);

ContextService.$inject = [];

function ContextService() {

    this.currentContexts = [];
}
