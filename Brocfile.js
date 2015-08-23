var mergeTrees = require("broccoli-merge-trees");
var funnel = require("broccoli-funnel");
var concat = require("broccoli-concat");
var env = require('broccoli-env').getEnv(); // BROCCOLI_ENV

var JSHinter = require('broccoli-jshint');
var esTranspiler = require("broccoli-babel-transpiler");
var iife = require("broccoli-iife");

var compileSass = require("broccoli-compass");
// var cleanCSS = require("broccoli-clean-css");
var csso = require('broccoli-csso');

var BrowserSync = require('broccoli-browser-sync');

//TODO: asset fingerprinting

var stylesTree = mergeTrees([
    funnel("app/assets/stylesheets", { include: ["*.scss"] }),
    funnel("app/assets/app", { include: ["**/*.scss"], destDir: "app" })
]);

var compiledStyles = compileSass(stylesTree, {
    outputStyle: "expanded",
    sassDir: ".",
});

var styles = concat(compiledStyles, {
    inputFiles: ["stylesheets/**/*.css"],
    outputFile: "/main.css"
});


var originalScripts = funnel("app/assets/app", { include: ["**/*.js"], destDir: "javascripts" });
var jsHintResults = new JSHinter(originalScripts);
var compiledScripts = iife(esTranspiler(originalScripts, { optional: ["es6.spec.symbols"] }));

var scripts = concat(compiledScripts, {
    inputFiles: ["javascripts/module.js", "javascripts/**/*.js"],
    outputFile: "/main.js",
    wrapInFunction: true
});

if (env === 'production') {
    module.exports = mergeTrees([
            csso(styles),
            scripts
    ]);
} else { // development
    var browserSync = new BrowserSync([styles, scripts], {
        // proxy the local play server
        port: 9000,
        browserSync: {
            open: false //TODO: does not work, always opens a browser
        }
    });

    module.exports = mergeTrees([
            styles, scripts,
            jsHintResults, browserSync
    ]);
}
