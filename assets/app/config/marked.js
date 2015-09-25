angular.module("wust.config").config(MarkedConfig);

MarkedConfig.$inject = [];

function MarkedConfig() {
    marked.setOptions({
        renderer: new marked.Renderer(),
        gfm: true,
        tables: true,
        breaks: false,
        pedantic: false,
        sanitize: false,
        smartLists: true,
        smartypants: false
    });
}
