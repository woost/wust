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
            let tags = node.classifications.concat(node.tags);
            // if ignoretags are set, we will filter by them (this is the case for streams and search.
            // otherwise the the current contexts are ignored.
            let filtered = _.reject(tags, i => _.any(scope.ignoreTags || ContextService.currentContexts, _.pick(i, "id")));
            return filtered[0];
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

