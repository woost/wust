angular.module("wust.elements").directive("editPopover", editPopover);

editPopover.$inject = ["$compile"];

function editPopover($compile) {
    return {
        priority: 1001, // compiles first
        terminal: true, // prevent lower priority directives to compile after it
        restrict: "A",
        scope: true,
        compile: function(el) {
            el.removeAttr("edit-popover"); // necessary to avoid in
            el.attr("content-template", "assets/app/elements/edit_popover/edit_popover.html");
            el.attr("title", "penos");
            el.attr("placement", "bottom");
            el.attr("auto-close", "1");
            el.attr("animation", "am-flip-x");
            el.attr("bs-popover", "");
            el[0].classList.add("edit_popover_directive");
            var fn = $compile(el);
            return function(scope) {
                fn(scope);
            };
        }
    };
}
