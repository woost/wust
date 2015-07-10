angular.module("wust.services").value("Helpers", {
    hashCode,
    lineIntersection,
    lineRectIntersection,
    clampLineByRects
});

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

function lineIntersection(line1, line2) {
    // if the lines intersect, the result contains the x and y of the intersection (treating the lines as infinite) and booleans for whether line segment 1 or line segment 2 contain the point
    // from: http://jsfiddle.net/justin_c_rounds/Gd2S2
    var denominator, a, b, numerator1, numerator2, result = {
        x: null,
        y: null,
        onLine1: false,
        onLine2: false
    };
    denominator = ((line2.end.y - line2.start.y) * (line1.end.x - line1.start.x)) - ((line2.end.x - line2.start.x) * (line1.end.y - line1.start.y));
    if (denominator === 0) {
        return result;
    }
    a = line1.start.y - line2.start.y;
    b = line1.start.x - line2.start.x;
    numerator1 = ((line2.end.x - line2.start.x) * a) - ((line2.end.y - line2.start.y) * b);
    numerator2 = ((line1.end.x - line1.start.x) * a) - ((line1.end.y - line1.start.y) * b);
    a = numerator1 / denominator;
    b = numerator2 / denominator;

    // if we cast these lines infinitely in both directions, they intersect here:
    result.x = line1.start.x + (a * (line1.end.x - line1.start.x));
    result.y = line1.start.y + (a * (line1.end.y - line1.start.y));
    /*
    // it is worth noting that this should be the same as:
    x = line2StartX + (b * (line2EndX - line2StartX));
    y = line2StartX + (b * (line2EndY - line2StartY));
    */
    // if line1 is a segment and line2 is infinite, they intersect if:
    if (a > 0 && a < 1) {
        result.onLine1 = true;
    }
    // if line2 is a segment and line1 is infinite, they intersect if:
    if (b > 0 && b < 1) {
        result.onLine2 = true;
    }
    // if line1 and line2 are segments, they intersect if both of the above are true
    return result;
}

function lineRectIntersection(line, rect) {
    let corners = {
        x0y0: {
            x: rect.x,
            y: rect.y
        },
        x1y0: {
            x: rect.x + rect.width,
            y: rect.y
        },
        x0y1: {
            x: rect.x,
            y: rect.y + rect.height
        },
        x1y1: {
            x: rect.x + rect.width,
            y: rect.y + rect.height
        }
    };

    let rectLines = [{
        start: corners.x0y0,
        end: corners.x1y0
    }, {
        start: corners.x0y0,
        end: corners.x0y1
    }, {
        start: corners.x1y1,
        end: corners.x1y0
    }, {
        start: corners.x1y1,
        end: corners.x0y1
    }];

    let rectIntersection = _.find(rectLines, rectLine => {
        let x = lineIntersection(line, rectLine);
        return x.onLine1 === true && x.onLine2 === true;
    });
    return rectIntersection;
}

function clampLineByRects(edge, sourceRect, targetRect) {
    let linkLine = {
        start: {
            x: edge.source.x,
            y: edge.source.y
        },
        end: {
            x: edge.target.x,
            y: edge.target.y
        }
    };

    let sourceIntersection = lineRectIntersection(linkLine,
        _.merge(sourceRect, {
            x: edge.source.x - sourceRect.width / 2,
            y: edge.source.y - sourceRect.height / 2
        }));

    let targetIntersection = lineRectIntersection(linkLine,
        _.merge(targetRect, {
            x: edge.target.x - targetRect.width / 2,
            y: edge.target.y - targetRect.height / 2
        }));

    return {
        x1: sourceIntersection === undefined ? edge.source.x : sourceIntersection.x,
        y1: sourceIntersection === undefined ? edge.source.y : sourceIntersection.y,
        x2: targetIntersection === undefined ? edge.target.x : targetIntersection.x,
        y2: targetIntersection === undefined ? edge.target.y : targetIntersection.y
    };
}
