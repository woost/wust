angular.module("wust.elements").directive("markdown", markdown);

markdown.$inject = [];

function markdown() {
    // ng-bind-html internally checks whether the html was explicitly trusted,
    // if not it checks whether ngSanitize is available and sanitizes the html
    // (otherwise an error is raised). We did not trust the output of the markdown
    // parser, thus it will be sanitized automatically.
    return {
        restrict: "EA",
        //TODO: declare background color in css
        replace: true,
        template: "<div class='well well-sm' style='border: 0; margin: 0px; height: 100%; overflow: auto; background-color:#FBFBFB' ng-bind-html='markdownHTML'></div>",
        scope: {
            markdown: "="
        },
        link
    };

    function link(scope) {
        scope.$watch("markdown", val => scope.markdownHTML = marked(val || ""));
    }
}
