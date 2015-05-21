angular.module("wust.graph").directive("d3Graph", d3Graph);

d3Graph.$inject = ["$window"];

function d3Graph($window) {
    return {
        restrict: "A",
        scope: {
            graph: "=",
            onClick: "&",
        },
        link: link
    };

    function link(scope, element) {
        let onDoubleClick = scope.onClick() || _.noop;

        // watch for changes in the ngModel
        scope.$watch("graph", graph => {
            if (graph.nodes === undefined)
                return;

            preprocessGraph(graph);

            // get dimensions
            let [width, height] = getElementDimensions(element[0]);

            // register for resize event
            angular.element($window).bind("resize", resizeGraph);

            // force configuration
            let force = d3.layout.force()
                .size([width, height])
                .linkStrength(3) // rigidity
                .friction(0.9)
                // .linkDistance(120) // weak geometric constraint. Pushes nodes to achieve this distance
                .linkDistance(d => connectsHyperEdge(d) ? 120 : 200)
                .charge(-1500)
                .gravity(0.1)
                .theta(0.8)
                .alpha(0.1);

            // remove any previous svg
            d3.select("svg").remove();

            // define events
            let zoom = d3.behavior.zoom().scaleExtent([0.1, 10]).on("zoom", zoomed);
            let drag = force.drag()
                .on("dragstart", ignoreHyperEdge(dragstarted))
                .on("dragend", ignoreHyperEdge(dragended))
                .on("drag", ignoreHyperEdge(dragged));

            // construct svg
            let svg = d3.select(element[0])
                .append("svg")
                .attr("width", width)
                .attr("height", height)
                .call(zoom)
                .on("dblclick.zoom", null);

            svg.append("svg:defs").append("svg:marker")
                .attr("id", "arrow")
                .attr("viewBox", "0 -3 10 6")
                .attr("refX", 10)
                .attr("markerWidth", 10)
                .attr("markerHeight", 6)
                .attr("orient", "auto")
                .append("svg:path")
                .attr("d", "M 0,-3 L 10,-0.5 L 10,0.5 L0,3")
                .attr("class", "svglink");

            // container with enabled pointer events
            let container = svg.append("g")
                .style("pointer-events", "all");

            // add nodes and edges
            force
                .nodes(graph.nodes)
                .links(graph.edges)
                .start();

            // create edges in the svg container
            let link = container.selectAll(".svglink")
                .data(graph.edges).enter()
                .append("line")
                .attr("class", "svglink");

            let linktextSvg = container.selectAll("g.linklabelholder")
                .data(graph.edges).enter()
                .append("g");

            let linktextFo = linktextSvg.append("foreignObject")
                .style("text-align", "center");

            let linktextHtml = linktextFo.append("xhtml:span")
                // .style("text-shadow", "white -1px 0px, white 0px 1px, white 1px 0px, white 0px -1px")
                .attr("class", "relation_label")
                .html(d => connectsHyperEdge(d) ? "" : d.title);

            let linktextRects = setForeignObjectDimensions(linktextFo, linktextHtml);

            // create nodes in the svg container
            let node = container.append("g")
                .selectAll(".svgnode")
                .data(graph.nodes).enter()
                .append("g")
                .call(drag)
                .on("dblclick", ignoreHyperEdge(onDoubleClick));

            let nodeFo = node.append("foreignObject")
                .style("text-align", "center");

            let nodeHtml = nodeFo.append("xhtml:div")
                .style("max-width", "150px")
                .style("cursor", d => d.hyperEdge ? "cursor" : "move")
                .attr("class", d => d.css)
                .html(d => d.title);

            let nodeRects = setForeignObjectDimensions(nodeFo, nodeHtml);

            // control whether tick function should draw
            let drawOnTick = false;
            svg.style("visibility", "hidden");

            // register tick function
            force.on("tick", tick);

            window.requestAnimationFrame(converge);

            // filter on event
            scope.$on("d3graph_filter", filter);

            // let the simulation converge
            let ia = 0;
            let startTime = Date.now();

            function converge() {
                // keep a constant frame rate
                while ((startTime + 300) > Date.now()) {
                    force.tick();
                    ia++;
                }

                if (force.alpha() > 0) {
                    window.requestAnimationFrame(converge);
                } else {
                    console.log("needed " + ia + " ticks to converge.");
                    drawOnTick = true;

                    // focusMarkedNodes needs visible/marked nodes and edges
                    _.each(graph.nodes, n => {
                        n.marked = true;
                        n.visible = true;
                    });
                    _.each(graph.edges, e => {
                        e.visible = true;
                    });

                    focusMarkedNodes(0);
                    drawGraph();
                    svg.style("visibility", "visible");
                }
            }

            // filter the graph
            function filter(event, filtered) {
                let filteredIds = _.map(filtered, "id");
                let ids = filteredIds;
                for (let i = 0; i < ids.length; i++) {
                    ids = _.union(ids, graph.edgeMap[ids[i]]);
                }

                graph.nodes = _.map(graph.nodes, node => {
                    let marked = _(filteredIds).includes(node.id);
                    let visible = marked || _(ids).includes(node.id);
                    return _.merge(node, {
                        visible,
                        marked
                    });
                });
                graph.edges = _.map(graph.edges, edge => {
                    let visible = _(ids).includes(edge.source.id) && _(ids).includes(edge.target.id);
                    return _.merge(edge, {
                        visible
                    });
                });

                setVisibility();
                focusMarkedNodes();
            }

            // reset visibility of nodes after filtering
            // TODO: set opacity of edges
            function setVisibility() {
                // set node visibility
                _.each(graph.nodes, (node, i) => {
                    let fo = nodeFo[0][i];
                    fo.style.opacity = (node.marked || node.hyperEdge) ? 1.0 : 0.3;
                    fo.style.visibility = node.visible ? "visible" : "hidden";
                });

                // set edge visibility
                _.each(graph.edges, (edge, i) => {
                    let line = link[0][i];
                    let fo = linktextFo[0][i];
                    let visibility = edge.visible ? "visible" : "hidden";
                    line.style.visibility = visibility;
                    fo.style.visibility = visibility;
                });
            }

            // focus the marked nodes and scale zoom accordingly
            function focusMarkedNodes(duration = 500) {
                let marked = _.select(graph.nodes, {
                    marked: true
                });
                if (_.isEmpty(marked)) {
                    return;
                }

                let min = [_.min(marked, "x").x, _.min(marked, "y").y];
                let max = [_.max(marked, "x").x, _.max(marked, "y").y];
                let center = [(max[0] + min[0]) / 2, (max[1] + min[1]) / 2];

                let scale;
                if (max[0] === min[0] || max[1] === min[1]) {
                    scale = 1;
                } else {
                    scale = Math.min(1, 0.9 * width / (max[0] - min[0]), 0.9 * height / (max[1] - min[1]));
                }

                let translate = [width / 2 - center[0] * scale, height / 2 - center[1] * scale];

                // skip animation if duration is zero
                if (duration > 0)
                    svg.transition().duration(duration).call(zoom.translate(translate).scale(scale).event);
                else
                    applyZoom(translate, scale);
            }

            // we need to set the height and weight of the foreignobject
            // to the dimensions of the inner html container.
            function setForeignObjectDimensions(fo, html) {
                return _.map(fo[0], (curr, i) => {
                    let rect = html[0][i].getBoundingClientRect();
                    //TODO: WORKAROUND
                    // why is rect.width == 0 for all hyperedge-nodes?
                    rect = {
                        width: rect.width || 70,
                        height: rect.height
                    };
                    curr.setAttribute("width", rect.width);
                    curr.setAttribute("height", rect.height);
                    return _.pick(rect, ["width", "height"]);
                });
            }

            // get the dimensions of a html element
            function getElementDimensions(elem) {
                return [elem.offsetWidth, elem.offsetHeight];
            }

            // resize graph according to the current element dimensions
            function resizeGraph() {
                [width, height] = getElementDimensions(element[0]);
                svg.attr("width", width);
                svg.attr("height", height);
                focusMarkedNodes();
            }

            // tick function, called in each step in the force calculation,
            // maps elements to positions
            function tick(e) {
                // push hypernodes towards the center between its start/end node
                let k = 10 * e.alpha;
                graph.nodes.forEach((o, i) => {
                    if (o.hyperEdge === true) {
                        let neighbours = graph.hyperNeighbours[o.id];
                        let start = neighbours.start;
                        let end = neighbours.end;
                        let center = {
                            x: (start.x + end.x) / 2,
                            y: (start.y + end.y) / 2
                        };
                        o.x += (center.x - o.x) * k;
                        o.y += (center.y - o.y) * k;
                    }
                });

                if (drawOnTick) {
                    drawGraph();
                }
            }

            function drawGraph() {
                // clamp every edge line to the intersections with its incident node rectangles
                link.each(function(link) {
                    d3.select(this).attr(clampEdgeLine(link));
                });

                node.attr("transform", d => {
                    // center the node
                    let rect = nodeRects[d.index];
                    return "translate(" + (d.x - rect.width / 2) + "," + (d.y - rect.height / 2) + ")";
                });

                linktextSvg.attr("transform", d => {
                    // center the linktext
                    let rect = linktextRects[d.index];
                    return "translate(" + (((d.source.x + d.target.x) / 2) - rect.width / 2) + "," + (((d.source.y + d.target.y) / 2) - rect.height / 2) + ")";
                });
            }

            // zoom into graph
            function zoomed() {
                applyZoom(d3.event.translate, d3.event.scale);
            }

            function applyZoom(translate, scale) {
                container.attr("transform", "translate(" + translate + ")scale(" + scale + ")");
            }

            // unfix the position of a given node
            function unsetFixedPosition(d) {
                d3.select(this).classed("fixed", d.fixed = false);
                // need to explicitly resume the force, otherwise the graph
                // is stuck until a node is dragged
                force.resume();
            }

            // fix the position of a given node
            function setFixedPosition(d) {
                d3.select(this).classed("fixed", d.fixed = true);

                force.resume();
            }

            // keep track whether the node is currently being dragged
            let isDragging = false;

            // we use dragend instead of click event, because it is emitted on mobile phones as well as on pcs
            function dragended(d) {
                if (isDragging) {
                    // if we were dragging before, the node should be fixed
                    setFixedPosition(d);
                } else {
                    // if the user just clicked, the position should be reset.
                    // we wait a moment before unsetting the fixed position in
                    // case the user wants to double click the node, so it does
                    // not float away beforehand.
                    _.delay(unsetFixedPosition, 180, d);
                }

                isDragging = false;
            }

            function dragstarted(d) {
                // prevent d3 from interpreting this as panning
                d3.event.sourceEvent.stopPropagation();
            }

            function dragged(d) {
                // check whether there was a substantial mouse movement. if
                // not, we will interpret this as a click event after the
                // mouse button is released (see dragended handler).
                // TODO: weight by current zoom level!
                let diff = Math.abs(d.x - d3.event.x) + Math.abs(d.y - d3.event.y);
                isDragging = isDragging || (diff > 1);

                // do the actually dragging
                d3.select(this).attr("cx", d.x = d3.event.x).attr("cy", d.y = d3.event.y);
            }

            // executes specified function only for normal nodes, i.e.,
            // ignores hyperedges
            function ignoreHyperEdge(func) {
                return d => {
                    // do nothing for hyperedges
                    if (d.hyperEdge)
                        return;

                    func(d);
                };
            }

            // check whether a link connects to a hyperedge-node
            function connectsHyperEdge(link) {
                return link.source.hyperEdge || link.target.hyperEdge;
            }

            // prepare graph for usage
            function preprocessGraph(graph) {
                // add index to edge
                // TODO: how to avoid this?  we need to access the
                // foreignobjects and html direcives through the edge
                _.each(graph.edges, (e, i) => e.index = i);

                // create edge map, maps node ids to connected node ids.
                // at this point, the source/target ids are not yet translated
                // into node objects by d3. thus, we reference nodes via the
                // given index in edge.source/target.
                graph.edgeMap = _(graph.edges).map(edge => {
                    let source = graph.nodes[edge.source].id;
                    let target = graph.nodes[edge.target].id;
                    return {
                        [source]: [target],
                        [target]: [source]
                    };
                }).reduce(_.partialRight(_.merge, (a, b) => {
                    return a ? a.concat(b) : b;
                }, _)) || {};

                let idToNode = _.indexBy(graph.nodes, "id");
                let hyperNodes = _.filter(graph.nodes, node => node.hyperEdge === true);
                //TODO: Map from node index to other node indices, to avoid string lookups
                graph.hyperNeighbours = _.indexBy(_.map(hyperNodes, node => {
                    return {
                        id: node.id,
                        start: idToNode[node.startId],
                        end: idToNode[node.endId]
                    };
                }), "id");
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

                let intersections = _.map(rectLines, rectLine => lineIntersection(line, rectLine));
                let rectIntersection = _.find(intersections, x => x.onLine1 === true && x.onLine2 === true);
                return rectIntersection;
            }

            function clampEdgeLine(edge) {
                let sourceNodeRect = nodeRects[edge.source.index];
                let targetNodeRect = nodeRects[edge.target.index];

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
                    _.merge(sourceNodeRect, {
                        x: edge.source.x - sourceNodeRect.width / 2,
                        y: edge.source.y - sourceNodeRect.height / 2
                    }));

                let targetIntersection = lineRectIntersection(linkLine,
                    _.merge(targetNodeRect, {
                        x: edge.target.x - targetNodeRect.width / 2,
                        y: edge.target.y - targetNodeRect.height / 2
                    }));

                return {
                    x1: sourceIntersection === undefined ? edge.source.x : sourceIntersection.x,
                    y1: sourceIntersection === undefined ? edge.source.y : sourceIntersection.y,
                    x2: targetIntersection === undefined ? edge.target.x : targetIntersection.x,
                    y2: targetIntersection === undefined ? edge.target.y : targetIntersection.y
                };
            }

        });
    }
}
