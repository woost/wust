angular.module("wust.elements").directive("coloredTag", coloredTag);

coloredTag.$inject = ["Helpers"];

function coloredTag(Helpers) {
    return {
        restrict: "EA",
        scope: {
            coloredTag: "=",
            intense: "@",
            border: "@"
        },
        link
    };

    function link(scope, elem) {
        let rawElem = elem[0];
        if(scope.intense === "true")
            rawElem.style.backgroundColor = Helpers.hashToColorFill(scope.coloredTag);
        else
            rawElem.style.backgroundColor = Helpers.hashToColorFillLight(scope.coloredTag);

        if(scope.border === "true")
            rawElem.style.borderColor = Helpers.hashToColorBorder(scope.coloredTag);

        // intense tags
        // rawElem.style.color = "white";
        // light tags
        rawElem.style.color = "black";
    }
}
