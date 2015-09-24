angular.module("wust.elements").directive("coloredTagSvgArrow", coloredTagSvgArrow);

coloredTagSvgArrow.$inject = ["Helpers"];

function coloredTagSvgArrow(Helpers) {
    return {
        restrict: "EA",
        scope: {
            coloredTagSvgArrow: "=",
            referenceNode: "=",
        },
        link
    };

    function link(scope, elem) {
        let rawElem = elem[0];

        scope.$watchCollection("coloredTagSvgArrow.tags", refreshColor);

        scope.$watchCollection(() => scope.referenceNode.classifications, refreshColor);

        function refreshColor() {
            setColor(selectTag(scope.coloredTagSvgArrow, scope.referenceNode));
        }

        function selectTag(node, referenceNode) {
            let connects = _.find(referenceNode.outRelations, h => h.endNode.id === node.id);
            let tags = Helpers.sortedNodeTags(connects);
            return tags[0];
        }

        function setColor(tag) {
            let arrowLine = rawElem.children[0];
            let arrowHead = rawElem.children[1];
            if (tag === undefined || tag.id === undefined) {
                arrowLine.style.stroke = "";
                arrowHead.style.fill = "";
            } else {
                arrowLine.style.fill = Helpers.classificationCircleBackgroundColor(tag);
                arrowLine.style.stroke = Helpers.classificationCircleBorderColor(tag);
                arrowHead.style.fill = Helpers.classificationCircleBackgroundColor(tag);
                arrowHead.style.stroke = Helpers.classificationCircleBorderColor(tag);
            }
        }
    }
}

