angular.module("wust.elements").directive("connectsArrow", connectsArrow);

connectsArrow.$inject = ["Helpers"];

function connectsArrow(Helpers) {
    return {
        restrict: "E",
        templateUrl: "elements/tag/connects-arrow.html",
        replace: true,
        transclude: true,
        scope: {
            source: "=",
            target: "=",
        },
        link
    };

    function link(scope, elem) {
        let rawElem = elem[0];

        scope.$watchCollection(() => scope.source.classifications, refreshColor);

        function refreshColor() {
            setColor(selectTag(scope.source, scope.target));
        }

        function selectTag(source, target) {
            let connects = _.find(source.outRelations, h => h.endNode.id === target.id);
            scope.classifications = Helpers.sortedNodeTags(connects);
            return scope.classifications[0];
        }

        function setColor(tag) {
            let svg = rawElem.children[2].children[0];
            let arrowLine = svg.children[0];
            let arrowHead = svg.children[1];
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

