angular.module("wust.elements").directive("descriptionPopover", descriptionPopover);

descriptionPopover.$inject = ["$compile", "$popover", "HistoryService"];

// This directive assumes that it should work on graph nodes (getWrap("graph"))
function descriptionPopover($compile, $popover, HistoryService) {
    return {
        priority: 1001, // compiles first
        terminal: true, // prevent lower priority directives to compile after it
        restrict: "A",
        scope: {
            node: "="
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

                let graph = HistoryService.currentViewComponent.getWrap("graph");
                let node = graph.nodeById(scope.nodeId);
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

                    let scale = graph.d3Graph.zoom.scale();
                    let popoverWidth = 400/scale;
                    popoverElem.style.minWidth = popoverWidth + "px";
                    popoverElem.style.maxWidth = popoverWidth + "px";
                    popoverElem.style.fontSize = (12/scale) + "px";

                    if (scope.enablePositionHack) {
                        popoverElem.style.top = node.rect.height + 10 + "px";
                        // sadly the markdown parser does not directly fill the
                        // html and therefore the client width is unknown,
                        // therefore we set the popover width statically to 400px
                        // popoverElem.style.left = (node.rect.width - popoverElem.clientWidth) / 2 + "px";
                        popoverElem.style.left = (node.rect.width - popoverWidth) / 2 + "px";
                    }
                };

                scope.enablePopover = function() {
                    popover.$promise.then(() => {
                        popover.show();
                    });
                };

                scope.disablePopover = function() {
                    popover.$promise.then(() => {
                        // when hovering really fast over the element, the
                        // popover was not created and therefore, there is no
                        // need to hide it.
                        if (!popover.$element)
                            return;

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
