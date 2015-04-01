angular.module("wust", [
    "ngResource",
    "ngAnimate",
    "ui.router",
    "ui.bootstrap",
    "ang-drag-drop",
    "xeditable",
]).run(function(editableOptions) {
    // http://vitalets.github.io/angular-xeditable/#overview
    editableOptions.theme = "bs3";
});
