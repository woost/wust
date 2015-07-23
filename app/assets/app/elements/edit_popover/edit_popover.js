angular.module("wust.elements").directive("editPopover", editPopover);

editPopover.$inject = ["$compile"];

function editPopover($compile) {
    return {
        priority: 1001, // compiles first
        terminal: true, // prevent lower priority directives to compile after it
        restrict: "A",
        scope: true,
        compile: function(el) {
            el.removeAttr("edit-popover"); // necessary to avoid infinite compile loop
            el.attr("popover", "'edit_popover.html'");
            el.attr("popover-title", "penos");
            el.attr("popover-placement", "bottom");
            el[0].classList.add("edit_popover_directive");
            var fn = $compile(el);
            return function(scope) {
                fn(scope);
            };
        }
    };
}
