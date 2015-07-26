angular.module("wust.elements").directive("descriptionPopover", descriptionPopover);

descriptionPopover.$inject = ["$compile", "$popover", "HistoryService"];

function descriptionPopover($compile, $popover, HistoryService) {
    return {
        priority: 1001, // compiles first
        terminal: true, // prevent lower priority directives to compile after it
        restrict: "A",
        scope: true,
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
                    popoverElem.style.top = attrs.positionHackHeight + "px";
                    // sadly the markdown parser does not directly fill the
                    // html and therefore the client width is unknown,
                    // therefore we set the popover width statically to 400px
                    // popoverElem.style.left = (attrs.positionHackWidth - popoverElem.clientWidth) / 2 + "px";
                    popoverElem.style.left = (attrs.positionHackWidth - 400) / 2 + "px";
                    popoverElem.style.minWidth = "400px";
                    popoverElem.style.maxWidth = "400px";
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

                scope.node = HistoryService.currentViewComponent.nodeById(attrs.nodeId);
                popover.$scope.node = scope.node;

                fn(scope);
            };
        }
    };
}
