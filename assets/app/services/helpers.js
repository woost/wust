angular.module("wust.services").value("Helpers", {
    fireWindowResizeEvent,
    mapFind,
    hashCode,
    sortedNodeTags,
    hashToColor,
    navBackgroundColor,
    postBorderColor,
    postBackgroundColor,
    classificationLabelBackgroundColor,
    classificationLabelBorderColor,
    classificationCircleBackgroundColor,
    classificationCircleBorderColor,
    contextLabelBackgroundColor,
    contextLabelBorderColor,
    contextCircleColor,
    contextCircleBorderColor,
    tagTitleColor,
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


function sortedNodeTags(node) {
    let sortByIdQuality = (tags) => _.sortBy(tags, ["id","quality"], ["asc", "desc"]);
    return sortByIdQuality(node.classifications).concat(sortByIdQuality(node.tags));
}

function navBackgroundColor(tag) { return hashToColor(tag, 20, 98); }

function postBackgroundColor(tag) { return hashToColor(tag, 20, 99); }
function postBorderColor(tag) { return hashToColor(tag, 20, 70); }

function classificationLabelBackgroundColor(tag) { return hashToColor(tag, 40, 90); }
function classificationLabelBorderColor(tag) { return hashToColor(tag, 40, 48); }
function classificationCircleBackgroundColor(tag) { return hashToColor(tag, 40, 90); }
function classificationCircleBorderColor(tag) { return hashToColor(tag, 40, 48); }

function contextLabelBackgroundColor(tag) { return hashToColor(tag, 30, 98); }
function contextLabelBorderColor(tag) { return hashToColor(tag, 20, 85); }
function contextCircleColor(tag) { return hashToColor(tag, 20, 98); }
function contextCircleBorderColor(tag) { return hashToColor(tag, 20, 85); }

function tagTitleColor(title) { return Math.abs(hashCode(title.toLowerCase())) % 360; }
function hashToColor(tag, chromaValue, lightness) {
    // https://vis4.net/blog/posts/avoid-equidistant-hsv-colors/
    let hue = tag.color || tagTitleColor(tag.title); // 0..360
    return chroma.hcl(hue, chromaValue, lightness).hex();
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
