angular.module("wust.elements").directive("tagLabel", tagLabel);

tagLabel.$inject = [];

function tagLabel() {
    return {
        restrict: "A",
        templateUrl: "elements/tag/tag_label.html",
        transclude: true,
        scope: {
            tagLabel: "=",
            disableLink: "@"
        },
        // link: function(scope, elem, attrs) {
        //     console.log(scope.vm.tagLabel.title, attrs);
        // },
        controller: tagLabelCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

tagLabelCtrl.$inject = [];

function tagLabelCtrl() {
    let vm = this;
}

