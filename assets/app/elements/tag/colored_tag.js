angular.module("wust.elements").directive("coloredTag", coloredTag);

coloredTag.$inject = ["Helpers"];

function coloredTag(Helpers) {
    return {
        restrict: "EA",
        // require: ["tagtype"], //TODO: require more attribute in directives
        scope: {
            coloredTag: "=",
            tagtype: "@"
        },
        link
    };

    function link(scope, elem) {
        let rawElem = elem[0];
        if (scope.coloredTag.$then)
            scope.coloredTag.$then(setStyle);
        else
            setStyle();

        function setStyle() {
            let tag = scope.coloredTag;
            let otherColor = Math.floor(Math.random()*360);
            let otherTag = {color: otherColor, title: ""};
            if(tag.isContext) {
                if(scope.tagtype === "label") {
                    rawElem.style.backgroundColor = Helpers.contextLabelBackgroundColor(tag);
                    rawElem.style.border = "1px solid " + Helpers.contextLabelBorderColor(tag);
                } else if(scope.tagtype === "circle") {
                    rawElem.style.backgroundColor = Helpers.contextCircleBackgroundColor(tag);
                    rawElem.style.border = "1px solid " + Helpers.contextCircleBorderColor(tag);
                    rawElem.style.borderRadius = Helpers.contextCircleBorderRadius();
                }
            } else { // classification
                scope.coloredTag.symbol.split(" ").forEach(className => {
                    if (!rawElem.classList.contains(className))
                        rawElem.classList.add(className);
                });

                if(scope.tagtype === "label") {
                    rawElem.style.backgroundColor = Helpers.classificationLabelBackgroundColor(tag);
                    rawElem.style.borderColor = Helpers.classificationLabelBorderColor(scope.coloredTag);
                    rawElem.style.borderRadius = Helpers.classificationLabelBorderRadius();
                } else if(scope.tagtype === "circle") {
                    // rawElem.style.backgroundColor = Helpers.classificationCircleBackgroundColor(tag);
                    // rawElem.style.border = "1px solid " + Helpers.classificationCircleBorderColor(tag);
                    // rawElem.style.color = Helpers.classificationCircleBorderColor(tag);
                }
            }
        }
    }
}
