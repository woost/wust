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
            rawElem.style.backgroundColor = Helpers.hashToHslFill(scope.coloredTag);
            rawElem.style.borderColor = Helpers.hashToHslBorder(scope.coloredTag);
        }
    }
}
