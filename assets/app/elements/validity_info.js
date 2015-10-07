angular.module("wust.elements").directive("validityInfo", validityInfo);

validityInfo.$inject = [];

function validityInfo() {
    return {
        restrict: "A",
        replace: true,
        template: "<div class='error_message' ng-style='{visibility: validityInfo.triedSave && !validityInfo.validity[property].valid && \"visible\" || \"hidden\"}' ng-bind='validityInfo.validity[property].message'></div>",
        scope: {
            validityInfo: "=",
            property: "@"
        }
    };
}
