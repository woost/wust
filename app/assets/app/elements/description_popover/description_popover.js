angular.module("wust.elements").directive("descriptionPopover", descriptionPopover);

descriptionPopover.$inject = ["$compile", "$popover", "HistoryService"];

// This directive assumes that it should work on graph nodes (getWrap("graph"))
function descriptionPopover($compile, $popover, HistoryService) {
    return {
        priority: 1001, // compiles first
        terminal: true, // prevent lower priority directives to compile after it
        restrict: "A",
        scope: {
            enablePositionHack: "@",
            nodeId: "@"
        },
        compile: function(el) {
            el.removeAttr("description-popover");
            el.attr("ng-mouseenter", "enablePopover()");
            el.attr("ng-mouseleave", "disablePopover()");
            el[0].classList.add("description_popover_directive");

            var fn = $compile(el);
            return function(scope, el, attrs) {
                let popover = $popover(el, {
                    "contentTemplate": "assets/app/elements/description_popover/description_popover.html",
                    // "title": "penos",
                    "placement": "bottom",
                    "autoClose": "0",
                    "animation": "",
                    "trigger": "manual",
                });

                let node = HistoryService.currentViewComponent.getWrap("graph").nodeById(scope.nodeId);
                popover.$scope.node = node;

                let elem = el[0];
                let origZindex = elem.style.zIndex;
                let origPopoverZindex;

                let originalApplyPlacement = popover.$applyPlacement;
                popover.$applyPlacement = function() {
                    originalApplyPlacement.apply(popover);

                    let popoverElem = popover.$element[0];
                    origPopoverZindex = popoverElem.style.zIndex;

                    elem.style.zIndex = 200;
                    popoverElem.style.zIndex = 300;

                    popoverElem.style.minWidth = "400px";
                    popoverElem.style.maxWidth = "400px";

                    if (scope.enablePositionHack) {
                        popoverElem.style.top = node.rect.height + "px";
                        // sadly the markdown parser does not directly fill the
                        // html and therefore the client width is unknown,
                        // therefore we set the popover width statically to 400px
                        // popoverElem.style.left = (node.rect.width - popoverElem.clientWidth) / 2 + "px";
                        popoverElem.style.left = (node.rect.width - 400) / 2 + "px";
                    }
                };

                scope.enablePopover = function() {
                    popover.$promise.then(() => {
                        popover.show();
                    });
                };

                scope.disablePopover = function() {
                    popover.$promise.then(() => {
                        let popoverElem = popover.$element[0];
                        elem.style.zIndex = origZindex;
                        popoverElem.style.zIndex = origPopoverZindex;
                        popover.hide();
                    });
                };

                fn(scope);
            };
        }
    };
}
