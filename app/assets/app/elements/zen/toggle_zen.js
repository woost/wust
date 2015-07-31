angular.module("wust.services").directive("toggleZen", toggleZen);

toggleZen.$inject = ["ZenService"];

function toggleZen(ZenService) {
    return {
        restrict: "A",
        templateUrl: "assets/app/elements/zen/toggle_zen.html",
        scope: {
            node: "=",
            service: "="
        },
        link
    };

    function link(scope) {
        scope.service = scope.service || ZenService;
        scope.preview = {
            active: false
        };
        scope.$watch("service.visible", val => {
            scope.preview.active = (val && scope.node === scope.service.node);
        });
    }
}
