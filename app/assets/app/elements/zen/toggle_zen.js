angular.module("wust.services").directive("toggleZen", toggleZen);

toggleZen.$inject = ["ZenService"];

function toggleZen(ZenService) {
    return {
        restrict: "A",
        template:
            "<button ng-if='zen.node !== node' class='btn btn-xs btn-primary' ng-click='zen.show(node)'><i class='fa fa-eye'></i></button>" +
            "<button ng-if='zen.node === node' class='btn btn-xs btn-primary' ng-click='zen.hide()'><i class='fa fa-eye-slash'></i></button>",
        scope: {
            node: "=",
        },
        link
    };

    function link(scope) {
        scope.zen = ZenService;
    }
}
