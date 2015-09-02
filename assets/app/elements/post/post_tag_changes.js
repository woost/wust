angular.module("wust.elements").directive("postTagChanges", postTagChanges);

postTagChanges.$inject = [];

function postTagChanges() {
    return {
        restrict: "A",
        templateUrl: "elements/post/post_tag_changes.html",
        scope: {
            postTagChanges: "="
        },
        controller: postTagChangesCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

postTagChangesCtrl.$inject = [];

function postTagChangesCtrl() {
    let vm = this;
}
