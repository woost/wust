angular.module("wust.elements").directive("coloredTagNode", coloredTagNode);

coloredTagNode.$inject = ["Helpers"];

function coloredTagNode(Helpers) {
    return {
        restrict: "EA",
        scope: {
            coloredTagNode: "=",
            ignoreTags: "="
        },
        link
    };

    function link(scope, elem) {
        let rawElem = elem[0];
        scope.$watchCollection("coloredTagNode.tags", tags => {
            return setColors(Helpers.sortTags(_.reject(tags, i => _.any(scope.ignoreTags, _.pick(i, "id"))))[0]);
        });

        function setColors(tag) {
            if (tag === undefined || tag.id === undefined) {
                rawElem.style.backgroundColor = "";
                rawElem.style.borderColor = "";
            } else {
                rawElem.style.backgroundColor = Helpers.hashToHslBackground(tag);
                rawElem.style.borderColor = Helpers.hashToHslBorder(tag);
            }
        }
    }
}
