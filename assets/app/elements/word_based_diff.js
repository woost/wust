angular.module("wust.elements").directive("wordBasedDiff", wordBasedDiff);

wordBasedDiff.$inject = [];

function wordBasedDiff() {
    return {
        restrict: "EA",
        scope: {
            sourceA: "=",
            sourceB: "="
        },
        link
    };

    function link(scope, element, attrs) {
        let rawElem = element[0];
        scope.$watch(() => scope.sourceA + "///" + scope.sourceB, () =>  {
            while( rawElem.hasChildNodes() ) rawElem.removeChild(rawElem.lastChild);

            var diff = JsDiff.diffWords(scope.sourceA, scope.sourceB);

            diff.forEach(function(part){
                // green for additions, red for deletions
                // grey for common parts
                var color = part.added ? "green" :
                    part.removed ? "#EF1B1B" : "#AAA";
                var span = document.createElement("span");
                span.style.color = color;
                span.appendChild(document.createTextNode(part.value));

                rawElem.appendChild(span);
            });
        });
    }
}
