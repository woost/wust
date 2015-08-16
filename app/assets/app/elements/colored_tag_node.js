angular.module("wust.elements").directive("coloredTagNode", coloredTagNode);

coloredTagNode.$inject = ["Helpers"];

function coloredTagNode(Helpers) {
    return {
        restrict: "EA",
        scope: {
            coloredTagNode: "="
        },
        link
    };

    function link(scope, elem) {
        let rawElem = elem[0];
        scope.$watch("coloredTagNode.tags", tags => {
            setColors(tags[0]);
        });

        function setColors(tag) {
            if (tag === undefined) {
                rawElem.style.backgroundColor = "";
                rawElem.style.borderColor = "";
            } else {
                rawElem.style.backgroundColor = Helpers.hashToHslBackground(tag);
                rawElem.style.borderColor = Helpers.hashToHslBorder(tag);
            }
        }
    }
}
