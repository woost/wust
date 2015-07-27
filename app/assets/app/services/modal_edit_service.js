angular.module("wust.services").service("ModalEditService", ModalEditService);

ModalEditService.$inject = ["$modal", "EditService"];

function ModalEditService($modal, EditService) {
    let modalInstance = $modal({
            show: false,
            templateUrl: "assets/app/elements/modal_edit/modal_edit.html",
            controller: "ModalEditCtrl",
            controllerAs: "vm",
            animation: "am-fade-and-slide-top"
    });

    this.show = showModal;
    this.hide = hideModal;

    let currentNode;
    //TODO: this does not work when the browser is reloaded and also it might be that we have removed the session from the scratchpad
    Object.defineProperty(this, "currentNode", {
        get: () => {
            if (currentNode === undefined || !currentNode.isLocal()) {
                currentNode = EditService.edit();
                return currentNode;
            } else {
                return currentNode;
            }
        }
    });

    function showModal() {
        modalInstance.$promise.then(modalInstance.show);
    }

    function hideModal() {
        modalInstance.$promise.then(modalInstance.hide);
    }
}

