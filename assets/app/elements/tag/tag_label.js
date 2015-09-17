angular.module("wust.elements").directive("tagLabel", tagLabel);

tagLabel.$inject = [];

function tagLabel() {
    return {
        restrict: "A",
        templateUrl: "elements/tag/tag_label.html",
        transclude: true,
        scope: {
            tagLabel: "=",
        },
        // link: function(scope, elem, attrs) {
        //     console.log(scope.vm.tagLabel.title, attrs);
        // },
        controller: tagLabelCtrl,
        controllerAs: "vm",
        bindToController: true,
        link: function (scope, elem, attrs) {
            scope.vm.disableLink = ("disableLink" in attrs) || scope.vm.tagLabel.isClassification;
        }
    };
}

tagLabelCtrl.$inject = [];

function tagLabelCtrl() {
    let vm = this;
}

