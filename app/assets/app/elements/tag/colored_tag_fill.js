angular.module("wust.elements").directive("coloredTagFill", coloredTagFill);

coloredTagFill.$inject = ["Helpers"];

function coloredTagFill(Helpers) {
    return {
        restrict: "EA",
        scope: {
            coloredTagFill: "="
        },
        link
    };

    function link(scope, elem) {
        let rawElem = elem[0];
        if (scope.coloredTagFill.id) {
            setColors();
        } else {
            rawElem.style.color = "black";
            let deregister = scope.$watch("coloredTagFill.id", () => {
                if (scope.coloredTagFill.id) {
                    setColors();
                    deregister();
                }
            });
        }

        function setColors() {
            rawElem.style.backgroundColor = Helpers.hashToHslFill(scope.coloredTagFill);
            rawElem.style.color = "white";
        }
    }
}
