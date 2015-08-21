var mergeTrees = require("broccoli-merge-trees");
var funnel = require("broccoli-funnel");
var concat = require("broccoli-concat");

var JSHinter = require('broccoli-jshint');
var esTranspiler = require("broccoli-babel-transpiler");
var iife = require("broccoli-iife");

var compileSass = require("broccoli-compass");
var cleanCSS = require("broccoli-clean-css");



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

var hintedScripts = new JSHinter(funnel("app/assets/app", {
    include: ["**/*.js"],
    destDir: "javascripts"
}));

var compiledScripts = iife(esTranspiler(hintedScripts));

var scripts = concat(compiledScripts, {
    inputFiles: ["javascripts/**/*.js"],
    outputFile: "/main.js",
    wrapInFunction: true
});

// Merge the compiled styles and scripts into one output directory.
module.exports = mergeTrees([styles, scripts]);
