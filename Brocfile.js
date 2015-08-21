var mergeTrees = require("broccoli-merge-trees");
var funnel = require("broccoli-funnel");
var concat = require("broccoli-concat");

var esTranspiler = require("broccoli-babel-transpiler");

var compileSass = require("broccoli-compass");
var cleanCSS = require("broccoli-clean-css");



//TODO: jshint
//TODO: asset fingerprinting
//TODO: css minify

var stylesTree = mergeTrees([
    funnel("app/assets/stylesheets", { include: ["*.scss"] }),
    funnel("app/assets/app", { include: ["**/*.scss"], destDir: "app"
    })
]);

var compiledStyles = compileSass(stylesTree, {
    outputStyle: "expanded",
    sassDir: ".",
});

var styles = concat(compiledStyles, {
    inputFiles: ["stylesheets/**/*.css"],
    outputFile: "/main.css"
});


var compiledScripts = esTranspiler(funnel("app/assets/app", {
    include: ["**/*.js"],
    destDir: "javascripts"
}));

var scripts = concat(compiledScripts, {
    inputFiles: ["javascripts/**/*.js"],
    outputFile: "/main.js",
    wrapInFunction: true
});

// Merge the compiled styles and scripts into one output directory.
module.exports = mergeTrees([styles, scripts]);
