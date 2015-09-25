angular.module("wust.elements").directive("wordBasedDiff", wordBasedDiff);

wordBasedDiff.$inject = [];

//TODO: used by title and description, but title shouldnt be markdown?
function wordBasedDiff() {
    return {
        restrict: "EA",
        scope: {
            sourceA: "=",
            sourceB: "="
        },
        link,
        //TODO: code dup from markdown.js
        template: "<div class='well well-sm' style='margin: 0px; background-color:#FBFBFB' ng-bind-html='markdown.html'></div>",
    };

    function link(scope, element, attrs) {
        let rawElem = element[0];
        scope.markdown = {};
        scope.$watch(() => scope.sourceA + "///" + scope.sourceB, () =>  {
            let diff = JsDiff.diffWords(scope.sourceA, scope.sourceB);

            let diffHTML = diff.map(function(part){
                // green for additions, red for deletions
                // grey for common parts
                var color = part.added ? "green" :
                    part.removed ? "red" : "#AAA";
                var span = document.createElement("span");
                span.style.color = color;
                span.appendChild(document.createTextNode(part.value));

                return span.outerHTML;
            }).reduce((a,b) => a + b, "");
            scope.markdown.html = marked(diffHTML);
        });
    }
}
