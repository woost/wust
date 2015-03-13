(function(Spinner) {
    app.directive("routeLoadingIndicator", function() {
        return {
            restrict: 'A',
            template: "<div ng-show='isRouteLoading' class='center-block loading_indicator'>" +
                "<div class='loading_indicator_body'>" +
                "<span class='h2'>Loading</span></span>" +
                "</div></div>",
            replace: true,
            link: function(scope, elem, attrs) {
                var opts = {
                    length: 12,
                    lines: 8,
                    radius: 12,
                    width: 8,
                };

                var spinner = new Spinner(opts);
                var target = elem[0];
                scope.isRouteLoading = false;

                scope.$on('$stateChangeStart', startSpinner);
                scope.$on('$stateChangeError', stopSpinner);
                scope.$on('$stateChangeSuccess', stopSpinner);

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
