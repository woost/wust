angular.module("wust.elements").directive("markdown", markdown);

markdown.$inject = [];

function markdown() {
    marked.setOptions({
        renderer: new marked.Renderer(),
        gfm: true,
        tables: true,
        breaks: false,
        pedantic: false,
        sanitize: false, // additionally escape all input html elements
        smartLists: true,
        smartypants: false
    });

    // ng-bind-html internally checks whether the html was explicitly trusted,
    // if not it checks whether ngSanitize is available and sanitizes the html
    // (otherwise an error is raised). We did not trust the output of the markdown
    // parser, thus it will be sanitized automatically.
    return {
        restrict: "EA",
        template: "<div class='well well-sm' style='margin: 0px;' ng-bind-html='markdownHTML'></div>",
        scope: {
            markdown: "="
        },
        link
    };

    function link(scope) {
        scope.$watch("markdown", val => scope.markdownHTML = marked(val || ""));
    }
}
