var env = require("broccoli-env").getEnv(); // BROCCOLI_ENV
var prod = env === "production";
var devDestDir = "target/web/public/main";

var fs = require("fs");
var mkdirp = require("mkdirp");

if (!prod)
mkdirp(devDestDir);

var concat = prod ? require("broccoli-concat") : require("broccoli-sourcemap-concat");
var mergeTrees = require("broccoli-merge-trees");
var funnel = require("broccoli-funnel");
var replace = require("broccoli-string-replace");
var esTranspiler = errorBuild("Babel", require("broccoli-babel-transpiler"));
var iife = require("broccoli-iife");
var closure = require("broccoli-closure");
var html2js = require("broccoli-html2js");
var compileSass = errorWrite("Compass", require("broccoli-compass"));
var csso = require("broccoli-csso");
var flatten = require('broccoli-flatten');

var stylesTree = mergeTrees([
    funnel("node_modules/bootstrap-sass/assets/stylesheets/bootstrap", {include: ["**/*.scss"], exclude: ["_variables.scss"], destDir: "bootstrap"}),
    funnel("assets/stylesheets", { include: ["*.scss", "bootstrap/_variables.scss"] }),
    funnel("assets/app", { include: ["**/*.scss"], destDir: "app" })
]);

var compiledStyles = compileSass(stylesTree, {
    ignoreErrors: false,
    outputStyle: "expanded",
    sassDir: ".",
});

var dependencies = mergeTrees(["node_modules", "bower_components", "static_assets/javascripts"], {overwrite: true});

var staticAssetsCss = funnel("static_assets", {
    include: [ "*.css" ],
    destDir: "static_assets_css"
});

var staticAssetsJs = funnel("static_assets", {
    include: [ "*.js" ],
    destDir: "static_assets_js"
});

var images = funnel("assets/images", {
    include: [ "*.png" ],
    destDir: "images"
});

var fonts = mergeTrees([
        flatten(funnel("bower_components", { include: ["font-awesome/fonts/*.woff*" ]}), {destDir: "fonts"}),
        funnel("static_assets", { include: [ "*.woff*" ], destDir: "fonts"}),
]);

var fontCss = replace(mergeTrees([
        funnel("bower_components", { include: [ "font-awesome/css/font-awesome.css" ]}),
        funnel(staticAssetsCss, { include: [ "static_assets_css/wust-font.css" ]}),
]),{
    files: [
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
        "font-awesome/css/font-awesome.css",
        "angular-motion/dist/angular-motion.css",
        "angular-ui-switch/angular-ui-switch.css",
        "ng-trans-css/ng-trans.css",
        "ng-sortable/dist/ng-sortable.css",
        "humane-js/themes/libnotify.css",
        "angular-xeditable/dist/css/xeditable.css",
        "highlightjs/styles/solarized_light.css",

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

var appScripts = iife(esTranspiler(appScriptsEs6));

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
        min("chroma-js/chroma.js"),

        "angular-native-dragdrop/draganddrop.js",
        min("ng-sortable/dist/ng-sortable.js"),
        min("humane-js/humane.js"),
        min("d3/d3.js"),

        min("angular-jwt/dist/angular-jwt.js"),
        min("angular-storage-no-cookies/dist/angular-storage.js"),
        min("angular-restmod/dist/angular-restmod-bundle.js"),
        min("angular-ui-switch/angular-ui-switch.js"),

        "highlightjs/highlight.pack.js",

        "lodium/lodium.js",
        "marked/marked.min.js",

        "ace-builds/src-min-noconflict/ace.js",
        "ace-builds/src-min-noconflict/mode-markdown.js",
        min("angular-xeditable/dist/js/xeditable.js"),
        min("angular-ui-ace/ui-ace.js"),
        "diff/dist/diff.js",
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
        images
    ]);
} else { // development
    var JSHinter = errorBuild("JsHint", require("broccoli-jshint"));
    var jsHintResults = JSHinter(appScriptsEs6);

    var BrowserSync = require("broccoli-browser-sync");
    browserSync = new BrowserSync([appScripts, htmlTemplates, styles], {
        // proxy the local play server
        port: 9000,
        browserSync: {
            open: false,
            notify: false
        }
    });

    module.exports = mergeTrees([
        styles, scripts, fonts, images, browserSync, jsHintResults, "assets/debug"
    ]);
}

function min(file) {
    if(prod)
        return file.slice(0,-2) + "min.js";
    else
        return file;
}

var errorObject = {};
function reportError(reporter, errors) {
    if (prod) {
        if (errors.length) {
            console.log(reporter + " failed, exiting.");
            process.exit(1);
        }
    } else {
        errorObject[reporter] = errorObject[reporter] || {};
        errorObject[reporter].errors = errors;
        if (errors.length)
            console.log(reporter + " failed, generating error message.");

        var errFile = devDestDir + "/errors.json";
        fs.writeFile(errFile, JSON.stringify(errorObject), function (err) {
            if (err) {
                console.error("Error writing error message to file", err);
            }
        });
    }
}

function errorWrap(property, reporter, plug) {
    if (prod)
        return plug;
    else
        return function(inputTree, options) {
            var self = new plug(inputTree, options);
            var func = self[property];
            self[property] = function() {
                var curr = func.apply(this, arguments)
                curr.then(function() {
                    reportError(reporter, self._errors || []);
                }).catch(function(message) {
                    reportError(reporter, message.toString().split("\n"));
                    browserSync.reload();
                });

                return curr;
            };

            return self;
        };
}

function errorWrite(reporter, plug) {
    return errorWrap("write", reporter, plug);
}

function errorBuild(reporter, plug) {
    return errorWrap("build", reporter, plug);
}
