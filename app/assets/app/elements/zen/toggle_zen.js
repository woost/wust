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
            // we have a session here from the edit_service, so that is ok.
            scope.preview.active = (val && ((scope.node.id === undefined && scope.node.localId === scope.service.node.localId) || scope.node.id === scope.service.node.id));
        });
    }
}
