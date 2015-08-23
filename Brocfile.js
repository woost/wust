var mergeTrees = require("broccoli-merge-trees");
var funnel = require("broccoli-funnel");
// var pickFiles = require('broccoli-static-compiler');
// var concat = require("broccoli-concat");
var concat = require("broccoli-sourcemap-concat");
var env = require('broccoli-env').getEnv(); // BROCCOLI_ENV

var JSHinter = require('broccoli-jshint');
var esTranspiler = require("broccoli-babel-transpiler");
var iife = require("broccoli-iife");

var compileSass = require("broccoli-compass");
// var cleanCSS = require("broccoli-clean-css");
var csso = require('broccoli-csso');

var BrowserSync = require('broccoli-browser-sync');

//TODO: asset fingerprinting
//TODO: sourcemaps

var stylesTree = mergeTrees([
    funnel("app/assets/stylesheets", { include: ["*.scss"] }),
    funnel("app/assets/app", { include: ["**/*.scss"], destDir: "app" })
]);

var compiledStyles = compileSass(stylesTree, {
    outputStyle: "expanded",
    sassDir: ".",
});

var styles = concat(mergeTrees([compiledStyles, "node_modules", "bower_components"], {overwrite: true}), {
    inputFiles: [
        "bootstrap-css-only/css/bootstrap.css",
        "angular-motion/dist/angular-motion.css",
        "angular-ui-switch/angular-ui-switch.css",
        "ng-trans-css/ng-trans.css",
        "ng-sortable/dist/ng-sortable.css",
        "humane-js/themes/libnotify.css",

        "stylesheets/**/*.css"
    ],
    outputFile: "/main.css"
});


var appScriptsEs6 = funnel("app/assets/app", { include: ["**/*.js"], destDir: "javascripts" });
var jsHintResults = new JSHinter(appScriptsEs6);
var appScripts = iife(esTranspiler(appScriptsEs6, { optional: ["es6.spec.symbols"] }));

var scripts = concat(mergeTrees([appScripts,"node_modules", "bower_components"], {overwrite: true}), {
    inputFiles: [
        "angular/angular.min.js",
        "angular-animate/angular-animate.js",
        "angular-sanitize/angular-sanitize.js",
        "angular-ui-router/release/angular-ui-router.js",
        "angular-bootstrap/ui-bootstrap.js",
        "angular-bootstrap/ui-bootstrap-tpls.js",
        "angular-strap/dist/angular-strap.js",
        "angular-strap/dist/angular-strap.tpl.js",

        "lodash/lodash.js",

        "angular-native-dragdrop/draganddrop.js",
        "ng-sortable/dist/ng-sortable.js",
        "humane-js/humane.js",
        "d3/d3.js",

        "angular-jwt/dist/angular-jwt.js",
        "angular-storage-no-cookies/dist/angular-storage.js",
        "angular-restmod/dist/angular-restmod-bundle.js",
        "angular-ui-switch/angular-ui-switch.js",

        "lodium/lodium.js",
        "marked/lib/marked.js",

        "angular-ui-ace/ui-ace.js",

        "ace-builds/src-min-noconflict/ace.js",
        "ace-builds/src-min-noconflict/mode-markdown.js",
        // "lib/ace-builds/src-min-noconflict/keybinding-vim.js",
        // "lib/ace-builds/src-min-noconflict/ext-language_tools.js",

        "javascripts/module.js",
        "javascripts/**/*.js"
    ],
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
