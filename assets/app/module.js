angular.module("wust", [
    "ngAnimate",
    "xeditable",
    "ui.bootstrap.collapse",
    "ui.bootstrap.typeahead",
    "template/typeahead/typeahead-popup.html",
    "template/typeahead/typeahead-match.html",
    "ui.bootstrap.tabs",
    "template/tabs/tab.html",
    "template/tabs/tabset.html",
    "ui.bootstrap.progressbar",
    "template/progressbar/progress.html",
    "template/progressbar/bar.html",
    "template/progressbar/progressbar.html",
    "mgcrea.ngStrap",
    "ang-drag-drop",
    "mp.deepBlur",
    "ui.ace",
    "as.sortable",
    "ngSanitize",
    "uiSwitch",
    "wust.config",
    "wust.filters",
    "wust.elements",
    "wust.components",
    "wust.templates"
]);

angular.module("wust.config", [
    "ui.router",
    "restmod",
    "wust.api",
    "wust.services",
    "wust.elements",
]);

angular.module("wust.api", [
    "restmod",
    "angular-jwt",
    "angular-storage"
]);

angular.module("wust.components", [
    "wust.services"
]);

angular.module("wust.elements", [
    "wust.services"
]);

angular.module("wust.services", [
    "angular-storage"
]);

angular.module("wust.filters", []);
