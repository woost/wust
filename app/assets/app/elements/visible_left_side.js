angular.module("wust.elements").directive("visibleLeftSide", visibleLeftSide);

visibleLeftSide.$inject = ["LeftSideService", "$compile"];

function visibleLeftSide(LeftSideService, $compile) {
    return {
        priority: 1001, // compiles first
        terminal: true, // prevent lower priority directives to compile after it
        scope: true,
        compile: function(el) {
            el.removeAttr("visible-left-side"); // necessary to avoid infinite compile loop
            el.attr("ng-class", "{visible: leftSide.visible}");
            var fn = $compile(el);
            return function(scope) {
                scope.leftSide = LeftSideService;
                fn(scope);
            };
        }
    };
}

