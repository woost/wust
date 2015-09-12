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

    function save() {
        if (currentNode === undefined)
            return;

        let promise = currentNode.save();
        if (currentNode.referenceNode === undefined)
            promise.$then(() => $state.go("focus", _.pick(currentNode, "id")));

        return promise;
    }

    function showModal(referenceNode) {
        if(referenceNode === undefined) {
            currentNode = EditService.editNewDiscussion(angular.copy(ContextService.currentContexts));
            modalInstance.$promise.then(modalInstance.show);
        } else {
            currentNode = EditService.editAnswer(referenceNode);
            modalInstance.$promise.then(modalInstance.show);
        }
    }

    function hideModal() {
        modalInstance.$promise.then(modalInstance.hide);
    }
}

