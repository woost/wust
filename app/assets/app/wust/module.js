// TODO: we should inject dependencies with string arrays instead of only using
// variable names, otherwise this will crash with minified code, which shortens
// variable names.
// TODO: should we gather ALL depenencies in one module? the app uses one
// injector for the whole app anyhow.
angular.module("wust", [
    "ngAnimate",
    "xeditable",
    "ui.router",
    "wust.api",
    "wust.discourse",
    "wust.components"
]);
