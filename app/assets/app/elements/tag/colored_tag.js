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
        if (scope.coloredTag.id) {
            setColors();
        } else {
            let deregister = scope.$watch("coloredTag.id", () => {
                if (scope.coloredTag.id) {
                    setColors();
                    deregister();
                }
            });
        }

        function setColors() {
            if(scope.intense === "true")
                rawElem.style.backgroundColor = Helpers.hashToHslFill(scope.coloredTag);
            else
                rawElem.style.backgroundColor = Helpers.hashToHslFillLight(scope.coloredTag);

            if(scope.border === "true")
                rawElem.style.borderColor = Helpers.hashToHslBorder(scope.coloredTag);
        }
    }
}
