angular.module("wust.config").config(HumaneConfig);

HumaneConfig.$inject = [];

function HumaneConfig() {
    marked.setOptions({
        renderer: new marked.Renderer(),
        gfm: true,
        tables: true,
        breaks: false,
        pedantic: false,
        sanitize: true, // additionally escape all input html elements
        smartLists: true,
        smartypants: false,
        highlight: function (code, lang) {
            return hljs.highlightAuto(code, lang ? [lang] : undefined).value;
        }
    });
}
