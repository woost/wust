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
        var diff = JsDiff.diffChars(scope.sourceA, scope.sourceB);

        diff.forEach(function(part){
            // green for additions, red for deletions
            // grey for common parts
            var color = part.added ? "green" :
                part.removed ? "red" : "#AAA";
            var span = document.createElement("span");
            span.style.color = color;
            span.appendChild(document.createTextNode(part.value));
            rawElem.appendChild(span);
        });
    }
}
