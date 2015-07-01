angular.module("wust.graph").directive("d3Graph", d3Graph);

d3Graph.$inject = ["$window", "DiscourseNode"];

function d3Graph($window, DiscourseNode) {
    return {
        restrict: "A",
        scope: {
            graph: "=",
            onClick: "&",
            onDraw: "&"
        },
        link: link
    };

    function link(scope, element) {
        let onDoubleClick = scope.onClick || _.noop;
        let onDraw = scope.onDraw || _.noop;

        // watch for changes in the ngModel
        scope.graph.$then(data => {
            let graph = angular.copy(data);
            preprocessGraph(graph);

            // get dimensions of containing element
            let [width, height] = [element[0].offsetWidth, element[0].offsetHeight];

            // svg will stay in background and only render the edges
            let svg = d3.select(element[0])
                .append("svg")
                .attr("width", width)
                .attr("height", height)
                .style("visibility", "hidden") // will be shown when converged
                .style("position", "absolute");
                // .style("background", "#FFDDAA")

            // has the same size and position as the svg
            // renders nodes and relation labels
            let html = d3.select(element[0])
                .append("div")
                .style("width", width + "px")
                .style("height", height + "px")
                .style("visibility", "hidden") // will be shown when converged
                .style("position", "absolute");
                // .style("background", "rgba(220, 240, 255, 0.5)")
                // .style("border", "1px solid #333")

            // marker for arrows
            svg.append("svg:defs").append("svg:marker")
                .attr("id", "graph_arrow")
                .attr("viewBox", "0 -3 10 6")
                .attr("refX", 10)
                .attr("markerWidth", 10)
                .attr("markerHeight", 6)
                .attr("orient", "auto")
                .append("svg:path")
                .attr("d", "M 0,-3 L 10,-0.5 L 10,0.5 L0,3")
                .attr("class", "svglink"); // for the stroke color

            // choose the correct transform style for many browsers
            var transformCompat = cssCompat("transform", "Transform", "transform");
            var transformOriginCompat = cssCompat("transformOrigin", "TransformOrigin", "transform-origin");
            function cssCompat(original, jsSuffix, cssSuffix) {
                if( !(original in document.body.style) ) {
                    if( ("Webkit" + jsSuffix) in document.body.style ) { return "-webkit-" + cssSuffix; }
                    if( ("Moz" + jsSuffix) in document.body.style ) { return "-moz-" + cssSuffix; }
                    if( ("ms" + jsSuffix) in document.body.style ) { return "-ms-" + cssSuffix; }
                    if( ("O" + jsSuffix) in document.body.style ) { return "-o-" + cssSuffix; }
                } else return cssSuffix;
            }

            // container with enabled pointer events
            // translates for zoom/pan will be applied here
            let svgContainer = svg.append("g");
            let htmlContainer = html.append("div")
                // html initially has its origin centered, svg has (top left)
                // fixes zooming
                .style(transformOriginCompat,"top left")
                .style("pointer-events", "all");

            // draw gravitational center
            // svgContainer.append("circle")
            //     .attr("cx",width/2)
            //     .attr("cy",height/2)
            //     .attr("r", 20);

            // register for resize event
            angular.element($window).bind("resize", resizeGraph);

            // force configuration
            let force = d3.layout.force()
                .size([width, height])
                .nodes(graph.nodes)
                .links(graph.edges)
                .linkStrength(3) // rigidity
                .friction(0.9)
                // .linkDistance(120) // weak geometric constraint. Pushes nodes to achieve this distance
                .linkDistance(d => connectsHyperEdge(d) ? 120 : 200)
                .charge(d => d.hyperEdge ? -1500 : -1500)
                .gravity(0.1)
                .theta(0.8)
                .alpha(0.1)
                .start();

            // define events
            let zoom = d3.behavior.zoom().scaleExtent([0.1, 10]).on("zoom", zoomed);
            let drag = force.drag()
                .on("dragstart", ignoreHyperEdge(dragstarted))
                .on("dragend", ignoreHyperEdge(dragended))
                .on("drag", ignoreHyperEdge(dragged));

            html.call(zoom)
                .on("dblclick.zoom", null);

            svg.on("dblclick.zoom", null);

            // create edges in the svg container
            let link = svgContainer.append("g").attr("id","group_links")
                .selectAll()
                .data(graph.edges).enter()
                .append("path")
                .attr("class", "svglink")
                .each(function(link) {
                    // if link is startRelation of a Hypernode
                    if( !(link.target.hyperEdge && link.target.startId === link.source.id) ) {
                        d3.select(this).style("marker-end", "url(" + location.href + "#graph_arrow)");
                    }
                });

            let linkText = svgContainer.append("div").attr("id","group_link_labels")
                .selectAll()
                .data(graph.edges).enter()
                .append("div");
            let linktextHtml = linkText.append("div")
                .attr("class", "relation_label")
                .html(d => connectsHyperEdge(d) ? "" : d.title);


            let node = htmlContainer.append("div").attr("id","group_hypernodes-then-nodes")
                .selectAll()
                .data(graph.nodes).enter()
                .append("div")
                .call(drag)
                .on("dblclick", ignoreHyperEdge(node => onDoubleClick({ node })));
            let nodeHtml = node.append("div")
                .style("position", "absolute")
                .style("max-width", "150px") // to produce line breaks
                .style("cursor", d => d.hyperEdge ? "cursor" : "move")
                .attr("class", d => d.css)
                .html(d => d.title);


            // control whether tick function should draw
            let drawOnTick = false;

            // register tick function
            force.on("tick", tick);

            requestAnimationFrame(converge);

            // filter on event
            scope.$on("d3graph_filter", filter);

            let linktextRects, nodeRects;
            function recalculateNodeDimensions() {
                nodeRects = cacheObjectDimensions(nodeHtml);
                linktextRects = cacheObjectDimensions(linktextHtml);
            }
            recalculateNodeDimensions();

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
                    requestAnimationFrame(converge);
                } else {
                    afterConverge();
                }
            }

            function afterConverge() {
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


                onDraw();
                html.style("visibility", "visible");
                svg.style("visibility", "visible");
                drawGraph();

                setTimeout(() => focusMarkedNodes(0), 400); //TODO: when is the right time to call focusMarkedNodes?
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

                _.each(graph.nodes, node => {
                    if(node.hyperEdge) {
                        //TODO: mark chains of hyperedges
                        node.marked = node.marked || graph.hyperNeighbours[node.id].start.marked && graph.hyperNeighbours[node.id].end.marked;
                    }
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
            function setVisibility() {
                let notMarkedOpacity = 0.3;
                // set node visibility
                _.each(graph.nodes, (node, i) => {
                    let domNode = nodeHtml[0][i];
                    domNode.style.opacity = (node.marked) ? 1.0 : notMarkedOpacity;
                    domNode.style.visibility = node.visible ? "inherit" : "hidden";
                });

                // set edge visibility
                _.each(graph.edges, (edge, i) => {
                    let path = link[0][i];
                    path.style.visibility = edge.visible ? "inherit" : "hidden";
                    path.style.opacity = (edge.source.marked === true && edge.target.marked === true) ? 1.0 : notMarkedOpacity;
                });
            }

            // focus the marked nodes and scale zoom accordingly
            function focusMarkedNodes(duration = 500) {
                if(width === 0 || height === 0) return;
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

                if (duration > 0) {
                    htmlContainer.transition().duration(duration).call(zoom.translate(translate).scale(scale).event);
                    svgContainer.transition().duration(duration).call(zoom.translate(translate).scale(scale).event);
                }
                else {
                    // skip animation if duration is zero
                    htmlContainer.call(zoom.translate(translate).scale(scale).event);
                    svgContainer.call(zoom.translate(translate).scale(scale).event);
                }

                // recalculateNodeDimensions();
                drawGraph();
            }

            // we need to set the height and weight of the foreignobject
            // to the dimensions of the inner html container.
            function cacheObjectDimensions(nodeHtml) {
                return _.map(nodeHtml[0], (curr) => {
                    return {
                        width: curr.offsetWidth,
                        height: curr.offsetHeight
                    };
                }
                );
            }

            // resize graph according to the current element dimensions
            function resizeGraph() {
                [width, height] = [element[0].offsetWidth, element[0].offsetHeight];
                svg.style("width", width).style("height", height);
                html.style("width", width + "px").style("height", height + "px");
                // if graph was hidden when initialized,
                // all foreign objects have size 0
                // this call recalculates the sizes
                focusMarkedNodes();
                recalculateNodeDimensions();
            }

            // tick function, called in each step in the force calculation,
            // maps elements to positions
            function tick(e) {
                // push hypernodes towards the center between its start/end node
                let hyperEdgePull = 10 * e.alpha;
                graph.nodes.forEach(node => {
                    if (node.hyperEdge === true) {
                        let neighbours = graph.hyperNeighbours[node.id];
                        let start = neighbours.start;
                        let end = neighbours.end;
                        let center = {
                            x: (start.x + end.x) / 2,
                            y: (start.y + end.y) / 2
                        };
                        node.x += (center.x - node.x) * hyperEdgePull;
                        node.y += (center.y - node.y) * hyperEdgePull;
                    }
                });

                if (drawOnTick) {
                    drawGraph();
                }
            }

            function drawGraph() {
                // clamp every edge line to the intersections with its incident node rectangles
                link.each(function(link) {
                    if( link.source.id === link.target.id ) { // self loop
                        //TODO: self loops with hypernodes
                        let rect = linktextRects[link.index];
                        d3.select(this).attr("d", `
                                M ${link.source.x} ${link.source.y - rect.height/2}
                                m -20, 0
                                c -80,-80   120,-80   40,0
                                `);
                    } else {
                        const line = clampEdgeLine(link);
                        // const pathAttr = `M 0 0 L ${line.x2 - line.x1} ${line.y2 - line.y1}`;
                        // d3.select(this).attr("d", pathAttr).style(transformCompat, `translate(${line.x1}px, ${line.y1}px)`);
                        const pathAttr = `M ${line.x1} ${line.y1} L ${line.x2} ${line.y2}`;
                        d3.select(this).attr("d", pathAttr);
                    }
                });

                node.style(transformCompat, d => {
                    // center the node on link ends
                    let rect = nodeRects[d.index];
                    return "translate(" + (d.x - rect.width / 2) + "px," + (d.y - rect.height / 2) + "px)";
                });

                linkText.style(transformCompat, d => {
                    // center the linktext
                    let rect = linktextRects[d.index];
                    if( d.source.id === d.target.id ) { // self loop
                        return "translate(" + (d.source.x - rect.width/2) + "px," + (d.source.y - rect.height/2 - 70) + "px)";
                    } else {
                        return "translate(" + (((d.source.x + d.target.x) / 2) - rect.width / 2) + "px," + (((d.source.y + d.target.y) / 2) - rect.height / 2) + "px)";
                    }

                });
            }

            // zoom into graph
            function zoomed() {
                applyZoom(d3.event.translate, d3.event.scale);
            }

            function applyZoom(translate, scale) {
                let transform = "translate(" + translate[0] + "px, " + translate[1] + "px) scale(" + scale + ")";
                svgContainer.style(transformCompat, transform);
                htmlContainer.style(transformCompat, transform);
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
                let diff = Math.abs(d.x - d3.event.x) + Math.abs(d.y - d3.event.y);
                isDragging = isDragging || (diff > 3);

                // do the actual dragging
                // d3.select(this).attr("cx", d.x = d3.event.x).attr("cy", d.y = d3.event.y);
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

                graph.idToNode = _.indexBy(graph.nodes, "id");
                graph.hyperNodes = _.filter(graph.nodes, node => node.hyperEdge === true);
                //TODO: Map from node index to other node indices, to avoid string lookups
                graph.hyperNeighbours = _.indexBy(_.map(graph.hyperNodes, node => {
                    return {
                        id: node.id,
                        start: graph.idToNode[node.startId],
                        end: graph.idToNode[node.endId]
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
