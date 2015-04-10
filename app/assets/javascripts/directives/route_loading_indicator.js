(function(Spinner) {
    angular.module("wust").directive("routeLoadingIndicator", function() {
        return {
            restrict: "A",
            template: "<div ng-show='isRouteLoading' class='center-block loading_indicator'>" +
                "<div class='loading_indicator_body'>" +
                "<span class='h2'>Loading</span></span>" +
                "</div></div>",
            replace: true,
            link: function(scope, elem) {
                let opts = {
                    length: 12,
                    lines: 8,
                    radius: 12,
                    width: 8,
                };

                let spinner = new Spinner(opts);
                let target = elem[0];
                scope.isRouteLoading = false;

                scope.$on("$stateChangeStart", startSpinner);
                scope.$on("$stateChangeError", stopSpinner);
                scope.$on("$stateChangeSuccess", stopSpinner);

                function startSpinner() {
                    scope.isRouteLoading = true;
                    spinner.spin(target);
                }

                function stopSpinner() {
                    scope.isRouteLoading = false;
                    spinner.stop();
                }
            }
        };
    });
})(Spinner);
