angular.module("wust.elements").directive("voteProgress", voteProgress);

voteProgress.$inject = [];

function voteProgress() {
    return {
        restrict: "E",
        templateUrl: "elements/vote_progress/vote_progress.html",
        transclude: true,
        scope: {
            change: "=",
        },
        controller: voteProgressCtrl,
        controllerAs: "vm",
        bindToController: true,
    };
}

voteProgressCtrl.$inject = [];

function voteProgressCtrl() {
    let vm = this;
}

