angular.module("wust.elements").directive("toggleZen", toggleZen);

toggleZen.$inject = ["ZenService"];

function toggleZen(ZenService) {
    return {
        restrict: "A",
        templateUrl: "zen/toggle_zen.html",
        scope: {
            node: "="
        },
        link
    };

    function link(scope) {
        scope.service = ZenService;
        scope.preview = {
            active: false
        };
        scope.$watch("service.visible", val => {
            // we have a session here from the edit_service, so that is ok.
            scope.preview.active = (val && ((scope.node.id === undefined && scope.node.localId === scope.service.node.localId) || scope.node.id === scope.service.node.id));
        });
    }
}
