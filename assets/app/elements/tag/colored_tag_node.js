angular.module("wust.elements").directive("coloredTagNode", coloredTagNode);

coloredTagNode.$inject = ["Helpers", "ContextService"];

function coloredTagNode(Helpers, ContextService) {
    return {
        restrict: "EA",
        scope: {
            coloredTagNode: "=",
            ignoreTags: "=",
            big: "@",
        },
        link
    };

    function link(scope, elem) {
        let rawElem = elem[0];

        if (scope.ignoreTags === undefined)
            scope.$on("context.changed", refreshColor);

        scope.$watchCollection(() =>
                scope.ignoreTags +
                scope.coloredTagNode.tags +
                scope.coloredTagNode.classifications,
                refreshColor);

        function refreshColor() {
            setColor(selectTag(scope.coloredTagNode));
        }

        function selectTag(node) {
            // if ignoretags are set, we will filter by them (this is the case for streams and search.
            // otherwise the the current contexts are ignored.
            let tags = Helpers.sortedNodeTags(node, scope.ignoreTags || ContextService.currentContexts);
            return tags[0];
        }

        function setColor(tag) {
            if (tag === undefined || tag.id === undefined) {
                rawElem.style.backgroundColor = "";
                rawElem.style.borderColor = "";
            } else {
                if( scope.big )
                    rawElem.style.backgroundColor = Helpers.bigPostBackgroundColor(tag);
                else
                    rawElem.style.backgroundColor = Helpers.smallPostBackgroundColor(tag);
                rawElem.style.borderColor = Helpers.postBorderColor(tag);
            }
        }
    }
}

