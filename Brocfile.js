var env = require("broccoli-env").getEnv(); // BROCCOLI_ENV
var prod = env === "production";

var concat = prod ? require("broccoli-concat") : require("broccoli-sourcemap-concat");
var mergeTrees = require("broccoli-merge-trees");
var funnel = require("broccoli-funnel");
var replace = require("broccoli-string-replace");
var JSHinter = require("broccoli-jshint");
var esTranspiler = require("broccoli-babel-transpiler");
var iife = require("broccoli-iife");
var closure = require("broccoli-closure");
var html2js = require("broccoli-html2js");
var compileSass = require("broccoli-compass");
var csso = require("broccoli-csso");
var BrowserSync = require("broccoli-browser-sync");
var flatten = require('broccoli-flatten');

var stylesTree = mergeTrees([
    funnel("assets/stylesheets", { include: ["*.scss"] }),
    funnel("assets/app", { include: ["**/*.scss"], destDir: "app" })
]);

var compiledStyles = compileSass(stylesTree, {
    outputStyle: "expanded",
    sassDir: ".",
});

var dependencies = mergeTrees(["node_modules", "bower_components"], {overwrite: true});

var staticAssetsCss = funnel("static_assets", {
    include: [ "**/*.css" ],
    destDir: "static_assets_css"
});

var staticAssetsJs = funnel("static_assets", {
    include: [ "**/*.js" ],
    destDir: "static_assets_js"
});

var images = funnel("assets", {
    include: [ "images/*.png" ]
});

var fonts = mergeTrees([
        flatten(funnel("bower_components", { include: [ "bootstrap-css-only/fonts/*.woff*", "font-awesome/fonts/*.woff*" ]}), {destDir: "fonts"}),
        funnel("static_assets", { include: [ "**/*.woff*" ], destDir: "fonts"}),
]);

var fontCss = replace(mergeTrees([
        funnel("bower_components", { include: [ "bootstrap-css-only/css/bootstrap.css", "font-awesome/css/font-awesome.css" ]}),
        funnel(staticAssetsCss, { include: [ "static_assets_css/wust-font.css" ]}),
]),{
    files: [
        "bootstrap-css-only/css/bootstrap.css",
        "font-awesome/css/font-awesome.css",
        "static_assets_css/wust-font.css"
    ],
    pattern: {
        match: /url\('..\/fonts?/g,
        replacement: "url('fonts"
    }
});

var styles = concat(mergeTrees([compiledStyles, dependencies, staticAssetsCss, fontCss], {overwrite: true}), {
    inputFiles: [
        "bootstrap-css-only/css/bootstrap.css",
        "font-awesome/css/font-awesome.css",
        "angular-motion/dist/angular-motion.css",
        "angular-ui-switch/angular-ui-switch.css",
        "ng-trans-css/ng-trans.css",
        "ng-sortable/dist/ng-sortable.css",
        "humane-js/themes/libnotify.css",

        "static_assets_css/**/*.css",

        "stylesheets/**/*.css"
    ],
    outputFile: "/main.css"
});

var htmlTemplates = html2js("assets/app", {
    inputFiles: ["**/*.html"],
    outputFile: "/templates.js",
    module: "wust.templates",
    singleModule: true,
    htmlmin: { collapseWhitespace: true }
});

var appScriptsEs6 = funnel("assets/app", { include: ["**/*.js"], destDir: "javascripts" });
var jsHintResults = new JSHinter(appScriptsEs6, {
    logError: function(message) {
        console.error(message);
        if (prod) {
            console.log("JsHint failed, exiting.");
            process.exit(1);
        }
    }
});
var appScripts = iife(esTranspiler(appScriptsEs6));

function min(file) {
    if(prod)
        return file.slice(0,-2) + "min.js";
    else
        return file;
}

var scripts = concat(mergeTrees([appScripts,htmlTemplates,dependencies,staticAssetsJs]), {
    inputFiles: [
        min("angular/angular.js"),
        min("angular-animate/angular-animate.js"),
        min("angular-sanitize/angular-sanitize.js"),
        min("angular-ui-router/release/angular-ui-router.js"),
        min("angular-bootstrap/ui-bootstrap.js"),
        min("angular-bootstrap/ui-bootstrap-tpls.js"),
        min("angular-strap/dist/angular-strap.js"),
        min("angular-strap/dist/angular-strap.tpl.js"),

        min("lodash/lodash.js"),

        "angular-native-dragdrop/draganddrop.js",
        min("ng-sortable/dist/ng-sortable.js"),
        min("humane-js/humane.js"),
        min("d3/d3.js"),

        min("angular-jwt/dist/angular-jwt.js"),
        min("angular-storage-no-cookies/dist/angular-storage.js"),
        min("angular-restmod/dist/angular-restmod-bundle.js"),
        min("angular-ui-switch/angular-ui-switch.js"),

        "lodium/lodium.js",
        "marked/marked.min.js",


        "ace-builds/src-min-noconflict/ace.js",
        "ace-builds/src-min-noconflict/mode-markdown.js",
        min("angular-ui-ace/ui-ace.js"),
        // "lib/ace-builds/src-min-noconflict/keybinding-vim.js",
        // "lib/ace-builds/src-min-noconflict/ext-language_tools.js",

        // "static_assets_js/**/*.js",

        "javascripts/module.js",
        "javascripts/**/*.js",
        "templates.js"
    ],
    outputFile: "/main.js"
});


if (prod) {
    module.exports = mergeTrees([
        csso(styles),
        closure(scripts, "main.js", {
            "language_in":         "ECMASCRIPT5",
            "language_out":        "ECMASCRIPT5",
            "warning_level":       "QUIET",
            "compilation_level":   "WHITESPACE_ONLY"
        }),
        fonts,
        images,
        jsHintResults
    ]);
} else { // development
    var browserSync = new BrowserSync([appScripts, htmlTemplates, styles], {
        // proxy the local play server
        port: 9000,
        browserSync: {
            open: false //TODO: does not work, always opens a browser
        }
    });

    module.exports = mergeTrees([
        styles, scripts, fonts, images, browserSync, jsHintResults
    ]);
}
