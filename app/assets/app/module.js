angular.module("wust", [
    "ngAnimate",
    "ui.bootstrap.collapse",
    "ui.bootstrap.typeahead",
    "template/typeahead/typeahead-popup.html",
    "template/typeahead/typeahead-match.html",
    "ui.bootstrap.tabs",
    "template/tabs/tab.html",
    "template/tabs/tabset.html",
    "mgcrea.ngStrap",
    "ang-drag-drop",
    "as.sortable",
    "ngSanitize",
    "uiSwitch",
    "xeditable",
    "wust.config",
    "wust.filters",
    "wust.elements",
    "wust.graph",
    "wust.components"
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
    "ui.ace",
    "wust.services"
]);

angular.module("wust.elements", [
    "wust.services"
]);

angular.module("wust.services", [
    "angular-storage"
]);

angular.module("wust.graph", []);

angular.module("wust.filters", []);
