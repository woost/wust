angular.module("wust.elements").directive("postChanges", postChanges);

postChanges.$inject = [];

function postChanges() {
    return {
        restrict: "A",
        templateUrl: "elements/post/post_changes.html",
        scope: {
            postChanges: "="
        },
        controller: postChangesCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

postChangesCtrl.$inject = [];

function postChangesCtrl(SidebarService, EditService, Session, ModalEditService) {
    let vm = this;
}
