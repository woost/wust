angular.module("wust.elements").service("ModalEditService", ModalEditService);

ModalEditService.$inject = ["$rootScope", "$modal", "EditService", "$state", "ContextService"];

function ModalEditService($rootScope, $modal, EditService, $state, ContextService) {
    let self = this;

    let modalInstance = $modal({
        show: false,
        templateUrl: "elements/modal_edit/modal_edit.html",
        controller: "ModalEditCtrl",
        controllerAs: "vm",
        animation: "am-fade-and-slide-top"
    });

    this.show = showModal;
    this.hide = hideModal;
    this.save = save;

    let currentNode;
    Object.defineProperty(this, "currentNode", {
        get: () => {
            return currentNode;
        }
    });

    function save(connectCallback) {
        if (currentNode === undefined)
            return;

        let promise = currentNode.save(connectCallback);
        if (currentNode.referenceNode === undefined)
            promise.$then(() => $state.go("focus", _.pick(currentNode, "id")));

        return promise;
    }

    function showModal(referenceNode) {
        if(referenceNode === undefined) {
            let start = {tags: angular.copy(ContextService.currentContexts)};
            currentNode = EditService.createSession(start);
            modalInstance.$promise.then(modalInstance.show);
        } else {
            currentNode = EditService.createSession({});
            modalInstance.$promise.then(modalInstance.show);
        }

        currentNode.setReference(referenceNode);
    }

    function hideModal() {
        modalInstance.$promise.then(modalInstance.hide);
    }
}

