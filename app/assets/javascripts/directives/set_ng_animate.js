// workaround for interference between ng-animate and bootstrap carousel
// individually disable/enable ng-animate for elements
// https://github.com/angular-ui/bootstrap/issues/1273
app.directive('setNgAnimate', function ($animate) {
    return {
        link: function ($scope, $element, $attrs) {
            $scope.$watch( function() {
                return $scope.$eval($attrs.setNgAnimate, $scope);
            }, function(valnew, valold){
                $animate.enabled(!!valnew, $element);
            });
        }
    };
});
