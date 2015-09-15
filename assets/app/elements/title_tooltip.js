angular.module("wust.elements").directive("tooltip", tooltip);

tooltip.$inject = ["$compile"];

function tooltip($compile) {
    return {
        restrict: "A",
        scope: false,
        link: function(scope, el, attrs) {
            let titleString = attrs.tooltip;
            el.removeAttr("tooltip"); // necessary to avoid infinite compile loop
            el.attr("data-title", titleString);
            el.attr("bs-tooltip", "");
            el.attr("animation", "");

            $compile(el)(scope);
        }
    };
}
