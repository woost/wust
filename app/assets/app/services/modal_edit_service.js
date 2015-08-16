angular.module("wust.services").service("ModalEditService", ModalEditService);

ModalEditService.$inject = ["$rootScope", "$modal", "EditService"];

function ModalEditService($rootScope, $modal, EditService) {
    let self = this;

    let modalInstance = $modal({
        show: false,
        templateUrl: "assets/app/elements/modal_edit/modal_edit.html",
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
            // if (currentNode === undefined || !currentNode.isLocal) {
            //     currentNode = EditService.edit();
            // }

            return currentNode;
        }
    });

    function save() {
        if (currentNode === undefined)
            return;

        currentNode.save();
    }

    function showModal() {
        currentNode = EditService.createSession();
        modalInstance.$promise.then(modalInstance.show);
    }

    function hideModal() {
        modalInstance.$promise.then(modalInstance.hide);
    }
}

