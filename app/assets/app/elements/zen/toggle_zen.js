angular.module("wust.services").directive("toggleZen", toggleZen);

toggleZen.$inject = ["ZenService"];

function toggleZen(ZenService) {
    return {
        restrict: "A",
        template:
            "<div ng-if='zen.node !== node' class='fa fa-eye-slash text-muted' style='cursor:pointer' ng-click='zen.show(node)' data-title='activate distraction free writing' bs-tooltip animation=''></div>" +
            "<div ng-if='zen.node === node' class='fa fa-eye' style='cursor:pointer' ng-click='zen.hide()' data-title='deactivate distraction free writing' bs-tooltip animation=''></div>",
        scope: {
            node: "=",
        },
        link
    };

    function link(scope) {
        scope.zen = ZenService;
    }
}
