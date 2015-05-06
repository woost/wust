// workaround for interference between ng-animate and bootstrap carousel
// individually disable/enable ng-animate for elements
// https://github.com/angular-ui/bootstrap/issues/1273
angular.module("wust").directive("setNgAnimate", setNgAnimate);

setNgAnimate.$inject = ["$animate"];

function setNgAnimate($animate) {
    return {
        link: link
    };

    function link($scope, $element, $attrs) {
        $scope.$watch(() => $scope.$eval($attrs.setNgAnimate, $scope), val => $animate.enabled(!!val, $element));
    }
}
