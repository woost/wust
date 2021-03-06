angular.module("wust.elements").service("TagRelationEditService", TagRelationEditService);

TagRelationEditService.$inject = ["$modal", "EditService"];

function TagRelationEditService($modal, EditService) {
    let self = this;

    let modalInstance = $modal({
        show: false,
        templateUrl: "elements/tag_relation/tag_relation.html",
        controller: "TagRelationCtrl",
        controllerAs: "vm",
        animation: "am-fade-and-slide-top"
    });

    this.show = showModal;
    this.hide = hideModal;
    this.save = save;
    this.disconnect = disconnect;

    let currentRelation, currentDisconnect;
    Object.defineProperty(this, "currentRelation", {
        get: () => {
            return currentRelation;
        }
    });

    function disconnect() {
        if (currentRelation === undefined)
            return;

        currentDisconnect();
    }

    function save() {
        if (currentRelation === undefined)
            return;

        currentRelation.save();
    }

    function showModal(node, disconnectFunc, isCreating = false) {
        currentRelation = EditService.editConnects(node);
        currentRelation.isCreating = isCreating;
        currentRelation.startNode = node.startNode;
        currentRelation.endNode = node.endNode;
        currentDisconnect = disconnectFunc;
        modalInstance.$promise.then(modalInstance.show);
    }

    function hideModal() {
        modalInstance.$promise.then(modalInstance.hide);
    }
}

