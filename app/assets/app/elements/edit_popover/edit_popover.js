angular.module("wust.elements").directive("editPopover", editPopover);

editPopover.$inject = ["$popover"];

function editPopover($popover) {
    return {
        priority: 1001, // compiles first
        terminal: true, // prevent lower priority directives to compile after it
        restrict: "A",
        scope: true,
        link: function(scope, el) {
            var myPopover = $popover(el, {
                "content-template": "assets/app/elements/edit_popover/edit_popover.html",
                "title": "penos",
                "placement": "bottom",
                "auto-close": "1",
                "animation": "am-flip-x",
            });
        }
    };
}
