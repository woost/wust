angular.module("wust.services").value("Helpers", {
    fireWindowResizeEvent,
    mapFind,
    hashCode,
    hashToColor,
    hashToColorBorder,
    hashToColorBackground,
    hashToColorFill,
    hashToColorFillLight,
    tagTitleColor,
    sortTags,
    cssCompat,
    coloredBorderWidth: "3px"
});

function fireWindowResizeEvent() {
    var evt = document.createEvent("UIEvents");
    evt.initUIEvent("resize", true, false,window,0);
    window.dispatchEvent(evt);
}

function mapFind(arr, mapFunc, findFunc) {
    for (let i = 0; i < arr.length; i++) {
        let mapped = mapFunc(arr[i]);
        if (findFunc(mapped))
            return mapped;
    }

    return undefined;
}

function hashCode(string) {
    let hash = 0;
    if (string.length === 0) return hash;
    for (let i = 0; i < string.length; i++) {
        let char = string.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash; // Convert to 32bit integer
    }
    return hash;
}

function hashToColorBorder(tag) {
    if(tag.color === -1) return "hsl(0, 0%, 45%)";
    return hashToColor(tag, 40, 45);
}

function hashToColorBackground(tag) {
    if(tag.color === -1) return "hsl(0, 0%, 98%)";
    return hashToColor(tag, 90, 95);
}

function hashToColorFill(tag) {
    if(tag.color === -1) return "hsl(0, 0%, 55%)";
    return hashToColor(tag, 75, 65);
}

function hashToColorFillLight(tag) {
    if(tag.color === -1) return "hsl(0, 0%, 55%)";
    return hashToColor(tag, 90, 80);
    // return hashToColor(tag, 57, 55);
}

function tagTitleColor(title) {
    return Math.abs(hashCode(title.toLowerCase())) % 360;
}

function hashToColor(tag, saturation, brightness) {
    // https://vis4.net/blog/posts/avoid-equidistant-hsv-colors/
    let hue = tag.color || tagTitleColor(tag.title); // 0..360
    let chroma = saturation / 100;//0.7; // 0..~5
    let lightness = brightness / 100;//1; // 0..~1.7
    return chroma.hcl(hue, chroma, lightness).hex();
}

function sortTags(tags) {
    let [remote,local] = _.partition(tags, "id");
    return orderTags(remote).concat(orderTags(local));

    function orderTags(tags) {
        return _.sortByOrder(tags,
            // make sorting deterministic
            ["isClassification","id"],
            ["desc","asc"]
        );
    }
}

function cssCompat(original, jsSuffix, cssSuffix) {
    if (!(original in document.body.style)) {
        if (("Webkit" + jsSuffix) in document.body.style) {
            return "-webkit-" + cssSuffix;
        }
        if (("Moz" + jsSuffix) in document.body.style) {
            return "-moz-" + cssSuffix;
        }
        if (("ms" + jsSuffix) in document.body.style) {
            return "-ms-" + cssSuffix;
        }
        if (("O" + jsSuffix) in document.body.style) {
            return "-o-" + cssSuffix;
        }
    } else return cssSuffix;
}
