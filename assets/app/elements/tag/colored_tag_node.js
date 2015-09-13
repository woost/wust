angular.module("wust.elements").directive("coloredTagNode", coloredTagNode);

coloredTagNode.$inject = ["Helpers", "ContextService", "$rootScope"];

function coloredTagNode(Helpers, ContextService, $rootScope) {
    return {
        restrict: "EA",
        scope: {
            coloredTagNode: "=",
            ignoreTags: "="
        },
        link
    };

    function link(scope, elem) {
        let rawElem = elem[0];

        if (scope.ignoreTags === undefined)
            $rootScope.$on("context.changed", refreshColor);

        scope.$watchCollection("coloredTagNode.tags", refreshColor);

        function refreshColor() {
            setColor(selectTag(scope.coloredTagNode));
        }

        function selectTag(node) {
            let tags = Helpers.sortedNodeTags(node, scope.ignoreTags || ContextService.currentContexts);
            // if ignoretags are set, we will filter by them (this is the case for streams and search.
            // otherwise the the current contexts are ignored.
            return tags[0];
        }

        function setColor(tag) {
            if (tag === undefined || tag.id === undefined) {
                rawElem.style.backgroundColor = "";
                rawElem.style.borderColor = "";
            } else {
                rawElem.style.backgroundColor = Helpers.postBackgroundColor(tag);
                rawElem.style.borderColor = Helpers.postBorderColor(tag);
            }
        }
    }
}

