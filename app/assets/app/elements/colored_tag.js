angular.module("wust.elements").directive("coloredTag", coloredTag);

coloredTag.$inject = ["Helpers"];

function coloredTag(Helpers) {
    return {
        restrict: "EA",
        scope: {
            coloredTag: "="
        },
        link
    };

    function link(scope, elem) {
        let rawElem = elem[0];
        rawElem.style.backgroundColor = Helpers.hashToHslBackground(scope.coloredTag);
        rawElem.style.borderColor = Helpers.hashToHslBorder(scope.coloredTag);
    }
}
