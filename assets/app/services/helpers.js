angular.module("wust.services").value("Helpers", {
    fireWindowResizeEvent,
    mapFind,
    hashCode,
    sortedNodeTags,
    hashToColor,
    navBackgroundColor,
    postBorderColor,
    smallPostBackgroundColor,
    bigPostBackgroundColor,
    classificationLabelBackgroundColor,
    classificationLabelBorderColor,
    classificationCircleBackgroundColor,
    classificationCircleBorderColor,
    contextLabelBackgroundColor,
    contextLabelBorderColor,
    contextCircleBackgroundColor,
    contextCircleBorderColor,
    classificationLabelBorderRadius,
    contextCircleBorderRadius,
    tagTitleColor,
    cssCompat,
    sortByIdQuality,
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

function sortByIdQuality(tags) {
    return _.sortByOrder(tags, ["quality", "id"], ["desc", "asc"]);
}

function withoutTags(tags, ignore) {
    let ignoreIds = ignore.map(t => t.id);
    return _.reject(tags, t => _.contains(ignoreIds, t.id));
}

function sortedNodeTags(node, ignore = []) {
    let classifications = _.uniq(node.classifications.concat(_.flatten(_.map(node.tags, "classifications"))), "id");
    return sortByIdQuality(withoutTags(classifications,ignore)).concat(sortByIdQuality(withoutTags(node.tags, ignore)));
}

function navBackgroundColor(tag) { return hashToColor(tag, 10, 98); }

function smallPostBackgroundColor(tag) { return hashToColor(tag, 20, 99); }
function bigPostBackgroundColor(tag) { return hashToColor(tag, 10, 99); }
function postBorderColor(tag) { return hashToColor(tag, 30, 68); }

function classificationLabelBackgroundColor(tag) { return hashToColor(tag, 40, 90); }
function classificationLabelBorderColor(tag) { return hashToColor(tag, 40, 48); }
function classificationLabelBorderRadius(tag) { return "12px";}
function classificationCircleBackgroundColor(tag) { return hashToColor(tag, 40, 90); }
function classificationCircleBorderColor(tag) { return hashToColor(tag, 40, 48); }

function contextLabelBackgroundColor(tag) { return hashToColor(tag, 25, 97); }
function contextLabelBorderColor(tag) { return hashToColor(tag, 30, 68); }
function contextCircleBackgroundColor(tag) { return hashToColor(tag, 25, 98); }
function contextCircleBorderColor(tag) { return hashToColor(tag, 30, 58); }
function contextCircleBorderRadius(tag) { return "1px";}

function tagTitleColor(title) { return wust.Shared().tagTitleColor(title); }
function hashToColor(tag, chroma, lightness) {
    // https://vis4.net/blog/posts/avoid-equidistant-hsv-colors/
    let hue = tag.color || tagTitleColor(tag.title); // 0..360
    return d3.hcl(hue, chroma, lightness).toString();
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
