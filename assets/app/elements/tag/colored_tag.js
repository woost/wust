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
            if(scope.tagtype === "circle") {
                if(tag.isClassification) {
                    let color = Helpers.hashToColorFill(tag);

                    rawElem.style.backgroundColor = Helpers.hashToColorFillLight(tag);
                    rawElem.style.border = "1px solid " + Helpers.hashToColorBorder(tag);
                }
                else {
                    let color = Helpers.contextCircleColor(tag);
                    rawElem.style.backgroundColor = color;
                    rawElem.style.border = "1px solid " + Helpers.contextCircleBorderColor(tag);
                }
            } else if(scope.tagtype === "label") {
                if(tag.isClassification) {
                    let color = Helpers.hashToColorFillLight(tag);
                    rawElem.style.backgroundColor = Helpers.hashToColorFillLight(tag);
                    rawElem.style.borderColor = Helpers.hashToColorBorder(scope.coloredTag);
                }
                else {
                    let color = Helpers.hashToColorContextLabelBg(tag);
                    rawElem.style.backgroundColor = color;
                    // let otherColor = Helpers.hashToColorFillLighter(tag);
                    // rawElem.style.background = `-webkit-linear-gradient(-55deg, ${otherColor}, ${otherColor} 22px, ${color} 22px, ${color} 60px)`;
                    rawElem.style.border = "1px solid " + Helpers.contextCircleBorderColor(tag);
                }
            }
        }
    }
}
