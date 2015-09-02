angular.module("wust.elements").directive("postEditChanges", postEditChanges);

postEditChanges.$inject = [];

function postEditChanges() {
    return {
        restrict: "A",
        templateUrl: "elements/post/post_edit_changes.html",
        scope: {
            postEditChanges: "="
        },
        controller: postEditChangesCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

postEditChangesCtrl.$inject = [];

function postEditChangesCtrl() {
    let vm = this;
}
