angular.module("wust.elements").directive("routeLoadingIndicator", routeLoadingIndicator);

routeLoadingIndicator.$inject = ["$rootScope"];

function routeLoadingIndicator($rootScope) {
    return {
        restrict: "A",
        template: "<div class='loading_indicator'></div>",
        replace: true,
        link: function(scope, elem, attrs) {
            let lodium = new Lodium(elem[0]);
            scope.isRouteLoading = false;

            $rootScope.$on("$stateChangeStart", () => {
                scope.isRouteLoading = true;
                lodium.start();
            });

            $rootScope.$on("$stateChangeError", () => {
                scope.isRouteLoading = false;
                lodium.stop();
            });

            $rootScope.$on("$stateChangeSuccess", () => {
                scope.isRouteLoading = false;
                lodium.stop();
            });
        }
    };
}
