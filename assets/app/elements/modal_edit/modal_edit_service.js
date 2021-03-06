angular.module("wust.elements").service("ModalEditService", ModalEditService);

ModalEditService.$inject = ["$modal", "EditService", "$state", "ContextService"];

function ModalEditService($modal, EditService, $state, ContextService) {
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

    let currentNode, editedComponentNode;
    Object.defineProperty(this, "currentNode", {
        get: () => {
            return currentNode;
        }
    });
    Object.defineProperty(this, "editedComponentNode", {
        get: () => {
            return editedComponentNode;
        }
    });

    function save() {
        if (currentNode === undefined)
            return;

        let promise = currentNode.save();
        if (promise) {
            if (currentNode.newDiscussion) {
                promise.$then(() => {
                    $state.go("focus", {
                        id: currentNode.id,
                        type: ""
                    });
                    hideModal();
                });
            } else {
                promise.$then(() => {
                    hideModal();
                });
            }
        }
    }

    function showModal(editableNode, isAnswer = true, tags = []) {
        if(editableNode === undefined) {
            currentNode = EditService.editNewDiscussion(angular.copy(tags));
            editedComponentNode = undefined;
        } else if (isAnswer) {
            currentNode = EditService.editAnswer(editableNode);
            editedComponentNode = undefined;
        } else {
            currentNode = EditService.edit(editableNode);
            editedComponentNode = editableNode;
        }
        modalInstance.$promise.then(modalInstance.show);
    }

    function hideModal() {
        modalInstance.$promise.then(modalInstance.hide);
    }
}

