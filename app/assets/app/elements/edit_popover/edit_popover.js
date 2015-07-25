angular.module("wust.elements").directive("editPopover", editPopover);

editPopover.$inject = ["$compile", "EditPopoverService"];

function editPopover($compile, EditPopoverService) {
    return {
        priority: 1001, // compiles first
        terminal: true, // prevent lower priority directives to compile after it
        restrict: "A",
        scope: true,
        compile: function(el) {
            //TODO: we should normally use the $popover service to construct a popover in a directive, but this does not work for content-template-url -> this seems to be a bug in angular-strap.
            el.removeAttr("edit-popover"); // necessary to avoid in
            el.attr("content-template", "assets/app/elements/edit_popover/edit_popover.html");
            el.attr("title", "penos");
            el.attr("placement", "bottom");
            el.attr("auto-close", "1");
            // el.attr("animation", "am-flip-x");
            el.attr("animation", "");
            el.attr("bs-popover", "");
            el[0].classList.add("edit_popover_directive");

            var fn = $compile(el);
            return function(scope, el) {
                scope.editPopoverService = EditPopoverService;
                fn(scope);
            };
        }
    };
}
