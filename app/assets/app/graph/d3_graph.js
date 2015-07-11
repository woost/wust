angular.module("wust.graph").directive("d3Graph", d3Graph);

d3Graph.$inject = ["$window", "DiscourseNode", "Helpers", "$location", "$filter"];

function d3Graph($window, DiscourseNode, Helpers, $location, $filter) {
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
        let onClick = scope.onClick || _.noop;
        let onDraw = scope.onDraw || _.noop;

        let graph = scope.graph;

        // bring nodes in order by calculating the difference between following and
        // leading nodes. Then assign numbers from -(nodes.length/2) to +(nodes.length/2).
        // This is used as force to pull nodes upwards or downwards.
        _(graph.nonHyperRelationNodes).each(node => {
            let deepReplies = node.deepSuccessors.length - node.deepPredecessors.length;
            node.verticalForce = deepReplies;
        }).sortBy("verticalForce").each((n,i) => n.verticalForce = i).value();

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

        // container with enabled pointer events
        // translates for zoom/pan will be applied here
        let svgContainer = svg.append("g").attr("id", "svgContainer");
        let htmlContainer = html.append("div").attr("id", "htmlContainer")
            // html initially has its origin centered, svg has (top left)
            // fixes zooming
            .style(transformOriginCompat, "top left");
        // .style("pointer-events", "all");

        // draw gravitational center
        svgContainer.append("circle")
            .attr("cx", width / 2)
            .attr("cy", height / 2)
            .attr("r", 20)
            .style("fill", "#7B00D6");

        // draw origin
        svgContainer.append("circle")
            .attr("cx", 0)
            .attr("cy", 0)
            .attr("r", 20);

        // define events
        let zoom = d3.behavior.zoom().scaleExtent([0.1, 10]).on("zoom", zoomed);
        let dragMove = d3.behavior.drag()
            .on("dragstart", ignoreHyperEdge(onDragMoveStart))
            .on("drag", ignoreHyperEdge(onDragMove))
            .on("dragend", ignoreHyperEdge(onDragMoveEnd));

        let dragConnect = d3.behavior.drag()
            .on("dragstart", ignoreHyperEdge(onDragConnectStart))
            .on("drag", ignoreHyperEdge(onDragConnectMove))
            .on("dragend", ignoreHyperEdge(onDragConnectEnd));

        let disableDrag = d3.behavior.drag()
            .on("dragstart", (d) => d3.event.sourceEvent.stopPropagation());

        html.call(zoom)
            .on("dblclick.zoom", null);

        svg.on("dblclick.zoom", null);

        // create edges in the svg container
        let link = svgContainer.append("g").attr("id", "group_links")
            .selectAll()
            .data(graph.edges).enter()
            .append("path")
            .attr("class", "svglink")
            .each(function(link) {
                // if link is startRelation of a Hypernode
                if (!(link.target.hyperEdge && link.target.startId === link.source.id)) {
                    d3.select(this).style("marker-end", "url(" + $location.absUrl() + "#graph_arrow)");
                }
            });

        // TODO: non-hyper-relation-links are broken
        // let linkText = svgContainer.append("div").attr("id", "group_link_labels")
        //     .selectAll()
        //     .data(graph.edges).enter()
        //     .append("div");
        // let linktextHtml = linkText.append("div")
        //     .attr("class", "relation_label")
        //     .html(d => connectsHyperEdge(d) ? "" : d.title);


        let node = htmlContainer.append("div").attr("id", "group_hypernodes-then-nodes")
            .attr("class", "nodecontainer")
            .selectAll()
            .data(graph.nodes).enter()
            .append("div")
            .style("pointer-events", "all");

        let nodeHtml = node.append("div")
            .attr("class", d => d.css)
            .style("position", "absolute")
            .style("max-width", "150px") // to produce line breaks
            .html(d => $filter("trim")(d.title, true, 50))
            // .style("border-width", n => Math.abs(n.verticalForce) + "px")
            // .style("border-color", n => n.verticalForce < 0 ? "#3CBAFF" : "#FFA73C")
            .style("cursor", d => d.hyperEdge ? "inherit" : "pointer")
            .on("click", ignoreHyperEdge(node => onClick({
                node
            })))
            .call(disableDrag);

        let nodeTools = node.append("div")
            .attr("class", "nodetools")
            .style("visibility", d => d.hyperEdge ? "hidden" : "inherit");

        let nodeDragTool = nodeTools.append("div")
            .attr("class", "nodetool dragtool fa fa-arrows")
            //TODO: browser-compatibility for grab and grabbed
            .style("cursor", d => d.hyperEdge ? "inherit" : "move")
            .call(dragMove);

        let nodeConnectTool = nodeTools.append("div")
            .attr("class","nodetool connecttool fa fa-compress")
            .style("cursor", d => d.hyperEdge ? "inherit" : "crosshair")
            .call(dragConnect);

        let connectorLine = svgContainer.append("line").classed({"connectorline":true});


        setInitialNodePositions();

        // write dom element ref into graph node
        // for easy lookup
        for(let i = 0; i < graph.nodes.length; i++) {
            graph.nodes[i].domElement = d3.select(node[0][i]);
        }

        // visibility of convergence
        let visibleConvergence = false;

        // control whether tick function should draw
        let drawOnTick = visibleConvergence;

        // register for resize event
        angular.element($window).bind("resize", resizeGraph);

        // force configuration
        let force = d3.layout.force()
            .size([width, height])
            .nodes(graph.nodes)
            .links(graph.edges)
            .linkStrength(0.9) // rigidity
            .friction(0.92)
            .linkDistance(100) // weak geometric constraint. Pushes nodes to achieve this distance
            // .linkDistance(d => connectsHyperEdge(d) ? 100 : 200)
            .charge(-1300)
            .chargeDistance(1000)
            .gravity(0.01)
            .theta(0.8)
            .alpha(0.1)
            .start();

        // register tick function
        force.on("tick", tick);

        let convergeIterations = 0;
        initConverge();
        if (visibleConvergence) {
            force.on("end", _.once(afterConverge)); // we don't know how to unsubscribe
        } else {
            requestAnimationFrame(converge);
        }

        // filter on event
        scope.$on("d3graph_filter", filter);

        function recalculateNodeDimensions() {
            cacheObjectDimensions(nodeHtml);
            // cacheObjectDimensions(linktextHtml);
        }
        recalculateNodeDimensions();

        function converge() {
            let startTime = Date.now();
            // keep a constant frame rate
            while (((startTime + 300) > Date.now()) && (force.alpha() > 0)) {
                force.tick();
                convergeIterations++;
            }

            if (force.alpha() > 0) {
                requestAnimationFrame(converge);
            } else {
                afterConverge();
            }
        }

        function initConverge() {
            // focusMarkedNodes needs visible/marked nodes and edges
            _.each(graph.nodes, n => {
                n.marked = true;
                n.visible = true;
            });
            _.each(graph.edges, e => {
                e.visible = true;
            });

            if (visibleConvergence) {
                recalculateNodeDimensions();
                focusMarkedNodes(0);
                html.style("visibility", "visible");
                svg.style("visibility", "visible");
            }
        }

        function afterConverge() {
            setFixed(graph.rootNode);

            drawOnTick = true;
            console.log("needed " + convergeIterations + " ticks to converge.");
            onDraw();
            if (visibleConvergence)
                focusMarkedNodes();
            else
                focusMarkedNodes(0);


            html.style("visibility", "visible");
            svg.style("visibility", "visible");
        }

        // filter the graph
        function filter(event, filtered) {
            let component = _(filtered).map(node => node.component).flatten().uniq().value();

            _.each(graph.nodes, node => {
                node.marked = _(filtered).contains(node);
                node.visible = node.marked || _(component).contains(node);

            });

            _.each(graph.nodes, node => {
                if (node.hyperEdge) {
                    //TODO: mark chains of hyperedges
                    node.marked = node.marked || node.source.marked && node.target.marked;
                }
            });

            _.each(graph.edges, edge => {
                edge.visible = _(component).contains(edge.source) && _(component).contains(edge.target);
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
                let domTool = nodeTools[0][i];
                let opacity = (node.marked) ? 1.0 : notMarkedOpacity;
                let visibility = node.visible ? "inherit" : "hidden";
                domNode.style.opacity = opacity;
                domNode.style.visibility = visibility;
                domTool.style.opacity = opacity;
                domTool.style.visibility = node.hyperEdge ? "hidden" : visibility;
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
            if (width === 0 || height === 0) return;
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
            } else {
                // skip animation if duration is zero
                htmlContainer.call(zoom.translate(translate).scale(scale).event);
                svgContainer.call(zoom.translate(translate).scale(scale).event);
            }

            drawGraph();
        }

        // we need to set the height and weight of the foreignobject
        // to the dimensions of the inner html container.
        function cacheObjectDimensions(nodeHtml) {
            _.each(nodeHtml[0], (curr) => {
                // __data__ contains the respective node/relation object
                curr.__data__.rect = {
                    width: curr.offsetWidth,
                    height: curr.offsetHeight
                };
            });
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
            let hyperEdgePull = e.alpha * 0.5;
            graph.nodes.forEach(node => {
                if (node.hyperEdge === true) {
                    let start = node.source;
                    let end = node.target;
                    let center = {
                        x: (start.x + end.x) / 2,
                        y: (start.y + end.y) / 2
                    };
                    let startDiffX = start.x - node.x;
                    let startDiffY = start.y - node.y;
                    let endDiffX = end.x - node.x;
                    let endDiffY = end.y - node.y;
                    node.x += (center.x - node.x) * hyperEdgePull;
                    node.y += (center.y - node.y) * hyperEdgePull;
                    let newStartDiffX = start.x - node.x;
                    let newStartDiffY = start.y - node.y;
                    let newEndDiffX = end.x - node.x;
                    let newEndDiffY = end.y - node.y;
                    if (start.fixed !== true) {
                        start.x += (startDiffX - newStartDiffX) * hyperEdgePull;
                        start.y += (startDiffX - newStartDiffX) * hyperEdgePull;
                    }
                    if (end.fixed !== true) {
                        end.x += (endDiffX - newEndDiffX) * hyperEdgePull;
                        end.y += (endDiffX - newEndDiffX) * hyperEdgePull;
                    }
                }
            });

            // pull nodes with more more children up
            graph.nonHyperRelationNodes.forEach(node => {
                if (node.fixed !== true) {
                    // let forceUp = node.outDegree - node.inDegree;
                    node.y += (node.verticalForce - graph.nonHyperRelationNodes.length / 2) * e.alpha * 1;
                }
            });

            if (drawOnTick) {
                drawGraph();
            }
        }

        function drawGraph() {
            let domNodes = node[0];
            for(let i = 0; i < domNodes.length; i++) {
                let domNode = domNodes[i];
                let graphNode = graph.nodes[i];
                domNode.style[transformCompat] = "translate(" + (graphNode.x - graphNode.rect.width / 2) + "px," + (graphNode.y - graphNode.rect.height / 2) + "px)";
            }

            let domLinks = link[0];
            for(let i = 0; i < domLinks.length; i++) {
                let domLink = link[0][i];
                let graphRelation = graph.edges[i];

                // draw svg paths for lines between nodes
                if (graphRelation.source.id === graphRelation.target.id) { // self loop
                    //TODO: self loops with hypernodes
                    let rect = graphRelation.rect;
                    domLink.setAttribute("d", `
                                M ${graphRelation.source.x} ${graphRelation.source.y - rect.height/2}
                                m -20, 0
                                c -80,-80   120,-80   40,0
                                `);
                } else {
                    // clamp every edge line to the intersections with its incident node rectangles
                    const line = Helpers.clampLineByRects(graphRelation, graphRelation.source.rect, graphRelation.target.rect);
                    const pathAttr = `M ${line.x1} ${line.y1} L ${line.x2} ${line.y2}`;
                    domLink.setAttribute("d", pathAttr);
                }


                // draw normal link-labels and center them
                // let domLinkTextNode = domLinks[i];
                // let rect = graphRelation.rect;
                // if (graphRelation.source.id === graphRelation.target.id) { // self loop
                //     domLinkTextNode.style[transformCompat] = "translate(" + (graphRelation.source.x - rect.width / 2) + "px," + (graphRelation.source.y - rect.height / 2 - 70) + "px)";
                // } else {
                //     domLinkTextNode.style[transformCompat] = "translate(" + (((graphRelation.source.x + graphRelation.target.x) / 2) - rect.width / 2) + "px," + (((graphRelation.source.y + graphRelation.target.y) / 2) - rect.height / 2) + "px)";
                // }
            }
        }

        // zoom into graph
        function zoomed() {
            applyZoom(d3.event.translate, d3.event.scale);
        }

        function applyZoom(translate, scale) {
            svgContainer.attr("transform", "translate(" + translate[0] + ", " + translate[1] + ") scale(" + scale + ")");
            htmlContainer.style(transformCompat, "translate(" + translate[0] + "px, " + translate[1] + "px) scale(" + scale + ")");
        }

        // unfix the position of a given node
        function unsetFixed(d) {
            d.fixed = false;
            d.domElement.classed({"fixed": false});

            // the fixed class could change the elements dimensions
            recalculateNodeDimensions();
        }

        // fix the position of a given node
        function setFixed(d) {
            d.fixed = true;
            d.domElement.classed({"fixed": true});

            // the fixed class could change the elements dimensions
            recalculateNodeDimensions();
        }

        // keep track whether the node is currently being dragged
        let isDragging = false;
        let dragStartNodeX, dragStartNodeY, dragOffsetX;
        let dragStartMouseX, dragStartMouseY, dragOffsetY;

        //TODO: rename d to something meaningful in all d3 code
        function onDragMoveStart(d) {
            let event = d3.event.sourceEvent;

            d.fixed |= 2; // copied from force.drag

            // prevent d3 from interpreting this as panning
            d3.event.sourceEvent.stopPropagation();

            dragStartNodeX = d.x;
            dragStartNodeY = d.y;
            dragStartMouseX = event.clientX;
            dragStartMouseY = event.clientY;

            d.domElement.classed({"moving" : true});
        }

        function onDragConnectStart(d) {
            let event = d3.event.sourceEvent;
            let scale = zoom.scale();

            // prevent d3 from interpreting this as panning
            d3.event.sourceEvent.stopPropagation();

            dragStartNodeX = d.x;
            dragStartNodeY = d.y;
            dragStartMouseX = event.clientX;
            dragStartMouseY = event.clientY;

            let domRect = d.domElement[0][0].childNodes[0].getBoundingClientRect();
            dragOffsetX = (event.srcElement.getBoundingClientRect().left - domRect.left) / scale + event.offsetX - d.domElement[0][0].childNodes[0].offsetWidth / 2;
            dragOffsetY = (event.srcElement.getBoundingClientRect().top - domRect.top) / scale + event.offsetY - d.domElement[0][0].childNodes[0].offsetHeight / 2;

            connectorLine
            .attr("x1", dragStartNodeX)
            .attr("y1", dragStartNodeY)
            .attr("x2", dragStartNodeX)
            .attr("y2", dragStartNodeY)
            .classed({"moving": true});
        }

        function onDragMove(d) {
            //TODO: fails when zooming and dragging at the same time
            let event = d3.event.sourceEvent;
            let scale = zoom.scale();

            // check whether there was a substantial mouse movement. if
            // not, we will interpret this as a click event after the
            // mouse button is released (see onDragMoveEnd handler).
            let diffX = dragStartMouseX - event.clientX;
            let diffY = dragStartMouseY - event.clientY;
            let diff = Math.sqrt(diffX * diffX + diffY * diffY);
            isDragging = isDragging || (diff > 5);

            if (isDragging) {
                // default positioning is center of node.
                // but we let node stay under grabbed position.
                d.px = dragStartNodeX + (event.clientX - dragStartMouseX) / scale;
                d.py = dragStartNodeY + (event.clientY - dragStartMouseY) / scale;
                force.resume(); // restart annealing
            }
        }

        function onDragConnectMove(d) {
            //TODO: fails when zooming and dragging at the same time
            let event = d3.event.sourceEvent;
            let scale = zoom.scale();

            // check whether there was a substantial mouse movement. if
            // not, we will interpret this as a click event after the
            // mouse button is released (see dragended handler).
            let diffX = dragStartMouseX - event.clientX;
            let diffY = dragStartMouseY - event.clientY;
            let diff = Math.sqrt(diffX * diffX + diffY * diffY);
            isDragging = isDragging || (diff > 5);

            // if (isDragging) {
                // default positioning is center of node.
                // but we let node stay under grabbed position.
                connectorLine
                .attr("x1", dragStartNodeX + dragOffsetX + (event.clientX - dragStartMouseX) /scale)
                .attr("y1", dragStartNodeY + dragOffsetY + (event.clientY - dragStartMouseY)/scale);
            // }
        }

        // we use dragend instead of click event, because it is emitted on mobile phones as well as on pcs
        function onDragConnectEnd(d) {
            // TODO: connect node
            // if (isDragging) {
            // } else {
            // }

            isDragging = false;

            connectorLine.classed({"moving" : false});
        }

        function onDragMoveEnd(d) {
            d.fixed &= ~6; // copied from force.drag
            if (isDragging) {
                // if we were dragging before, the node should be fixed
                setFixed(d);
            } else {
                // if the user just clicked, the position should be reset.
                unsetFixed(d);
            }

            isDragging = false;

            d.domElement.classed({"moving" : false});
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

        function setInitialNodePositions() {
            let squareFactor = 100 * Math.sqrt(graph.nodes.length);
            _(graph.nonHyperRelationNodes).each((n, i) => {
                let hash = Math.abs(Helpers.hashCode(n.id));
                n.x = squareFactor * (hash & 0xfff) / 0xfff + width / 2 - squareFactor / 2;
                n.y = squareFactor * n.verticalForce/graph.nonHyperRelationNodes.length + height / 2 - squareFactor / 2;
            }).value();

            _(graph.hyperRelations).each((n, i) => {
                n.x = (n.source.x + n.target.x) / 2;
                n.y = (n.source.y + n.target.y) / 2;
            }).value();
        }
    }
}
