angular.module("wust.services").service("TagRelationEditService", TagRelationEditService);

TagRelationEditService.$inject = ["$rootScope", "$modal", "EditService"];

function TagRelationEditService($rootScope, $modal, EditService) {
    let self = this;

    let modalInstance = $modal({
        show: false,
        templateUrl: "assets/app/components/tags/tag_relation.html",
        controller: "TagRelationCtrl",
        controllerAs: "vm",
        animation: "am-fade-and-slide-top"
    });

    this.show = showModal;
    this.hide = hideModal;
    this.save = save;

    let currentRelation;
    Object.defineProperty(this, "currentRelation", {
        get: () => {
            return currentRelation;
        }
    });

    function save() {
        if (currentRelation === undefined)
            return;

        currentRelation.save();
    }

    function showModal(node) {
        currentRelation = EditService.createSession(node, false);
        modalInstance.$promise.then(modalInstance.show);
    }

    function hideModal() {
        modalInstance.$promise.then(modalInstance.hide);
    }
}

