angular.module("wust.elements").directive("coloredTag", coloredTag);

coloredTag.$inject = ["Helpers"];

function coloredTag(Helpers) {
    return {
        restrict: "EA",
        scope: {
            coloredTag: "=",
            intense: "@",
            border: "@"
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
            if(scope.intense === "true") { // tag circles
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
            } else { // tag labels
                if(tag.isClassification) {
                    let color = Helpers.hashToColorFillLight(tag);
                    rawElem.style.backgroundColor = Helpers.hashToColorFillLight(tag);
                    if(scope.border === "true")
                        rawElem.style.borderColor = Helpers.hashToColorBorder(scope.coloredTag);
                }
                else {
                    let color = Helpers.hashToColorContextLabelBg(tag);
                    let otherColor = Helpers.hashToColorFillLighter(tag);
                    rawElem.style.background = `-webkit-linear-gradient(-55deg, ${otherColor}, ${otherColor} 22px, ${color} 22px, ${color} 60px)`;
                    // rawElem.style.backgroundColor = color;
                    if(scope.border === "true")
                        rawElem.style.border = "1px solid " + Helpers.contextCircleBorderColor(tag);
                }
            }


            // intense tags
            // rawElem.style.color = "white";
            // light tags
            rawElem.style.color = "black";
        }
    }
}
