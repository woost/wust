angular.module("wust", [
    "ui.bootstrap",
    "ngAnimate",
    "ui.layout",
    "ngSanitize",
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
    "wust.services"
]);

angular.module("wust.api", [
    "restmod",
    "angular-jwt",
    "angular-storage"
]);

angular.module("wust.components", [
    "ang-drag-drop",
    "ui.ace",
    "wust.services"
]);

angular.module("wust.elements", [
    "ang-drag-drop",
    "wust.services"
]);

angular.module("wust.services", [
    "angular-storage"
]);

angular.module("wust.graph", []);

angular.module("wust.filters", []);
