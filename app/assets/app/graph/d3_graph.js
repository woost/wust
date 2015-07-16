angular.module("wust.graph").directive("d3Graph", d3Graph);

d3Graph.$inject = ["$window", "DiscourseNode", "Helpers", "$location", "$filter", "Post"];

function d3Graph($window, DiscourseNode, Helpers, $location, $filter, Post) {
    return {
        restrict: "A",
        scope: {
            graph: "=",
            onClick: "&",
            onDraw: "&"
        },
        link
    };

    function link(scope, element) {
        let onClick = scope.onClick || _.noop;
        let onDraw = scope.onDraw || _.noop;

        let graph = scope.graph;
        let rootDomElement = element[0];

        let globalState = {
            // settings
            visibleConvergence: false,
            debugDraw: false,
            hyperRelationAlignForce: 0.5,
            nodeVerticalForce: 1,

            // state
            drawOnTick: false,
            hoveredNode: undefined,
            width: rootDomElement.offsetWidth,
            height: rootDomElement.offsetHeight,
        };

        let force = d3.layout.force()
            .size([globalState.width, globalState.height])
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
            .alpha(0.1);

        let dragState = {
            isDragging: false,
            dragStartNode: undefined,
            dragStartNodeX: undefined,
            dragStartNodeY: undefined,
            dragStartMouseX: undefined,
            dragStartMouseY: undefined,
            dragStartOffsetX: undefined,
            dragStartOffsetY: undefined
        };

        globalState.drawOnTick = globalState.visibleConvergence;


        // svg will stay in background and only render the edges
        let d3Svg = d3.select(rootDomElement)
            .append("svg")
            .attr("width", globalState.width)
            .attr("height", globalState.height)
            .style("position", "absolute");
        // .style("background", "#FFDDAA")

        // has the same size and position as the svg
        // renders nodes and relation labels
        let d3Html = d3.select(rootDomElement)
            .append("div")
            .style("width", globalState.width + "px")
            .style("height", globalState.height + "px")
            .style("position", "absolute");
        // .style("background", "rgba(220, 240, 255, 0.5)")
        // .style("border", "1px solid #333")

        // marker for arrows
        d3Svg.append("svg:defs").append("svg:marker")
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
        let transformCompat = cssCompat("transform", "Transform", "transform");
        let transformOriginCompat = cssCompat("transformOrigin", "TransformOrigin", "transform-origin");


        // container with enabled pointer events
        // translates for zoom/pan will be applied here
        let d3SvgContainer = d3Svg.append("g").attr("id", "svgContainer")
            .style("visibility", "hidden"); // will be shown when converged
        let d3HtmlContainer = d3Html.append("div").attr("id", "htmlContainer")
            // html initially has its origin centered, svg has (top left)
            // fixes zooming
            .style(transformOriginCompat, "top left")
            .style("visibility", "hidden"); // will be shown when converged
        // .style("pointer-events", "all");

        if (globalState.debugDraw) {
            // draw gravitational center
            d3SvgContainer.append("circle")
                .attr("cx", globalState.width / 2)
                .attr("cy", globalState.height / 2)
                .attr("r", 20)
                .style("fill", "#7B00D6");

            // draw origin
            d3SvgContainer.append("circle")
                .attr("cx", 0)
                .attr("cy", 0)
                .attr("r", 20);
        }



        let d3NodeContainer = d3HtmlContainer.append("div").attr("id", "hypernodes-then-nodes")
            .attr("class", "nodecontainer");

        let d3LinkPath = d3SvgContainer.append("g").attr("id", "linkContainer");

        let d3ConnectorLine = d3SvgContainer.append("line").classed({
            "connectorline": true
        });

        // register tick function
        force.on("tick", _.partial(tick, graph, globalState, transformCompat));

        let zoom = d3.behavior.zoom().scaleExtent([0.1, 10]);


        //////////////////////////////////////////////////

        updateGraph();
        graph.onCommit(updateGraph);

        converge(globalState, force, graph, zoom, d3HtmlContainer, d3SvgContainer, onDraw, transformCompat);

        //////////////////////////////////////////////////

        function updateGraph(changes) {
            console.log("------ update graph");
            console.log(graph.nonHyperRelationNodes.map((n) => n.title), graph.hyperRelations.map((r) => r.source.title + " --> " + r.target.title));
            // create data joins
            // http://bost.ocks.org/mike/join/
            let d3NodeContainerWithData = d3NodeContainer
                .selectAll("div")
                .data(graph.nodes, (d) => d.id);

            let d3LinkPathWithData = d3LinkPath
                .selectAll("path")
                .data(graph.edges, (d) => d.startId + " --> " + d.endId);

            // add nodes
            let d3NodeFrame = d3NodeContainerWithData.enter()
                .append("div")
                .style("pointer-events", "all");

            let d3Node = d3NodeContainerWithData.append("div")
                .attr("class", d => d.hyperEdge ? "relation_label" : `node ${DiscourseNode.get(d.label).css}`)
                .style("position", "absolute")
                .style("max-width", "150px") // to produce line breaks
                .html(d => $filter("trim")(d.title, true, 50))
                // .style("border-width", n => Math.abs(n.verticalForce) + "px")
                // .style("border-color", n => n.verticalForce < 0 ? "#3CBAFF" : "#FFA73C")
                .style("cursor", d => d.hyperEdge ? "inherit" : "pointer");

            // add relations
            d3LinkPathWithData.enter()
                .append("path")
                .attr("class", "svglink")
                .each(function(link) {
                    // if link is startRelation of a Hypernode
                    if (!(link.target.hyperEdge && link.target.startId === link.source.id)) {
                        d3.select(this).style("marker-end", "url(" + $location.absUrl() + "#graph_arrow)");
                    }
                });

            let d3NodeTools = d3NodeContainerWithData.append("div")
                .attr("class", "nodetools");

            let d3NodeDragTool = d3NodeTools.append("div")
                .style("display", d => d.hyperEdge ? "none" : "inline-block")
                .attr("class", "nodetool dragtool fa fa-arrows")
                //TODO: browser-compatibility for grab and grabbed
                .style("cursor", "move");

            let d3NodeConnectTool = d3NodeTools.append("div")
                .style("display", d => d.hyperEdge ? "none" : "inline-block")
                .attr("class", "nodetool connecttool fa fa-compress")
                .style("cursor", "crosshair");

            let d3NodeDisconnectTool = d3NodeTools.append("div")
                .style("display", d => d.hyperEdge ? "inline-block" : "none")
                .attr("class", "nodetool connecttool fa fa-expand")
                .style("cursor", "pointer");

            /// remove nodes and relations
            d3NodeContainerWithData.exit().remove();
            d3LinkPathWithData.exit().remove();//

            console.log(graph.nodes.map(n => n.id.slice(0,3)),d3NodeContainer.node());
            console.log(graph.edges,d3LinkPath.node());

            // TODO: non-hyper-relation-links are broken
            // let linkText = svgContainer.append("div").attr("id", "group_link_labels")
            //     .selectAll()
            //     .data(graph.edges).enter()
            //     .append("div");
            // let linktextHtml = linkText.append("div")
            //     .attr("class", "relation_label")
            //     .html(d => connectsHyperEdge(d) ? "" : d.title);
            // check whether a link connects to a hyperedge-node
            // function connectsHyperEdge(link) {
            //     return link.source.hyperEdge || link.target.hyperEdge;
            // }

            setInitialNodePositions(globalState, graph);
            updateGraphRefs(graph, d3NodeContainerWithData, d3Node, d3NodeTools, d3LinkPathWithData);
            registerUIEvents();
            calculateNodeVerticalForce(graph);
            recalculateNodeDimensions(graph);
            // force.tick();
            // drawGraph(graph, transformCompat);
            force.start();

            function registerUIEvents() {
                // define events
                zoom.on("zoom", _.partial(zoomed, d3SvgContainer, d3HtmlContainer, transformCompat));
                let dragMove = d3.behavior.drag()
                    .on("dragstart", ignoreHyperEdge(_.partial(onDragMoveStart, dragState, zoom)))
                    .on("drag", ignoreHyperEdge(_.partial(onDragMove, globalState, dragState, zoom, force)))
                    .on("dragend", ignoreHyperEdge(_.partial(onDragMoveEnd, dragState, force, graph)));

                let dragConnect = d3.behavior.drag()
                    .on("dragstart", ignoreHyperEdge(_.partial(onDragConnectStart, globalState, dragState, zoom, d3ConnectorLine)))
                    .on("drag", ignoreHyperEdge(_.partial(onDragConnectMove, globalState, dragState, zoom, d3ConnectorLine)))
                    .on("dragend", ignoreHyperEdge(_.partial(onDragConnectEnd, globalState, dragState, graph, d3ConnectorLine)));

                let disableDrag = d3.behavior.drag()
                    .on("dragstart", () => d3.event.sourceEvent.stopPropagation());

                d3Html.call(zoom).on("dblclick.zoom", null);

                d3Svg.on("dblclick.zoom", null);

                d3Node.on("click", ignoreHyperEdge(node => {
                        console.log("click");
                        onClick({
                            node
                        });
                    }))
                    .call(disableDrag)
                    .on("mouseover", d => globalState.hoveredNode = d)
                    .on("mouseout", d => {
                        globalState.hoveredNode = undefined;
                        d.d3NodeContainer.classed({
                            "selected": false
                        });
                    });

                d3NodeDragTool.call(dragMove);
                d3NodeConnectTool.call(dragConnect);
                d3NodeDisconnectTool.on("click", _.partial(disconnectHyperRelation, graph));

                // register for resize event
                angular.element($window).bind("resize", _.partial(resizeGraph, graph, globalState, zoom, rootDomElement, d3SvgContainer, d3HtmlContainer, transformCompat));

                // filter on event
                scope.$on("d3graph_filter", _.partial(filter, globalState, graph, zoom, d3SvgContainer, d3HtmlContainer));
            }
        }
    }


    function onDragStartInit(dragState, zoom, d) {
        // prevent d3 from interpreting this as panning
        d3.event.sourceEvent.stopPropagation();

        let event = d3.event.sourceEvent;
        let scale = zoom.scale();

        dragState.dragStartNodeX = d.x;
        dragState.dragStartNodeY = d.y;
        dragState.dragStartMouseX = event.clientX;
        dragState.dragStartMouseY = event.clientY;
        dragState.dragStartNode = d;

        let domRect = d.domNode.getBoundingClientRect();
        let eventRect = event.srcElement.getBoundingClientRect();
        dragState.dragOffsetX = (eventRect.left - domRect.left) / scale + event.offsetX - d.domNode.offsetWidth / 2;
        dragState.dragOffsetY = (eventRect.top - domRect.top) / scale + event.offsetY - d.domNode.offsetHeight / 2;
    }

    //TODO: rename d to something meaningful in all d3 code
    function onDragMoveStart(dragState, zoom, d) {
        onDragStartInit(dragState, zoom, d);

        d.fixed |= 2; // copied from force.drag
    }

    function onDragConnectStart(globalState, dragState, zoom, d3ConnectorLine, d) {
        onDragStartInit(dragState, zoom, d);

        d3ConnectorLine
            .attr("x1", dragState.dragStartNodeX)
            .attr("y1", dragState.dragStartNodeY)
            .attr("x2", dragState.dragStartNodeX)
            .attr("y2", dragState.dragStartNodeY)
            .classed({
                "moving": true
            });
    }

    function onDragMoveInit(dragState, d, onStartDragging = () => {}) {
        // check whether there was a substantial mouse movement. if
        // not, we will interpret this as a click event after the
        // mouse button is released (see onDragMoveEnd handler).
        let diffX = dragState.dragStartMouseX - event.clientX;
        let diffY = dragState.dragStartMouseY - event.clientY;
        let diff = Math.sqrt(diffX * diffX + diffY * diffY);
        if (!dragState.isDragging) {
            if (diff > 5) {
                dragState.isDragging = true;
                onStartDragging();
            }
        }
    }

    function onDragMove(globalState, dragState, zoom, force, d) {
        //TODO: fails when zooming/scrolling and dragging at the same time
        onDragMoveInit(dragState, d, () => d.d3NodeContainer.classed({
            "moving": true
        }));

        if (dragState.isDragging) {
            // default positioning is center of node.
            // but we let node stay under grabbed position.
            let event = d3.event.sourceEvent;
            let scale = zoom.scale();
            d.px = dragState.dragStartNodeX + (event.clientX - dragState.dragStartMouseX) / scale;
            d.py = dragState.dragStartNodeY + (event.clientY - dragState.dragStartMouseY) / scale;
            force.resume(); // restart annealing
        }
    }

    function onDragConnectMove(globalState, dragState, zoom, d3ConnectorLine, d) {
        //TODO: fails when zooming/scrolling and dragging at the same time
        let event = d3.event.sourceEvent;
        let scale = zoom.scale();

        onDragMoveInit(dragState, d);

        if (dragState.isDragging) {
            // default positioning is center of node.
            // but we let node stay under grabbed position.
            d3ConnectorLine
                .attr("x1", dragState.dragStartNodeX + dragState.dragOffsetX + (event.clientX - dragState.dragStartMouseX) / scale)
                .attr("y1", dragState.dragStartNodeY + dragState.dragOffsetY + (event.clientY - dragState.dragStartMouseY) / scale);

            if (globalState.hoveredNode !== undefined) {
                globalState.hoveredNode.d3NodeContainer.classed({
                    "selected": true
                });
                dragState.dragStartNode.d3NodeContainer.classed({
                    "selected": true
                });
            } else {
                dragState.dragStartNode.d3NodeContainer.classed({
                    "selected": false
                });
            }
        }
    }

    // we use dragend instead of click event, because it is emitted on mobile phones as well as on pcs
    function onDragConnectEnd(globalState, dragState, graph, d3ConnectorLine) {
        if (dragState.isDragging) {
            if (globalState.hoveredNode !== undefined) {
                console.log("connect:", dragState.dragStartNode, globalState.hoveredNode);
                let start = Post.$buildRaw(dragState.dragStartNode.$encode());
                start.connectsTo.$buildRaw(globalState.hoveredNode.$encode()).$save({}).$then(response => {
                    _.each(response.graph.nodes, n => graph.addNode(n));
                    _.each(response.graph.edges, r => graph.addRelation(r));
                    graph.commit();
                });
            }
        }
        // TODO: else { connect without dragging only by clicking }
        // TODO: create self loops?

        dragState.isDragging = false;

        d3ConnectorLine.classed({
            "moving": false
        });
        dragState.dragStartNode.d3NodeContainer.classed({
            "selected": false
        });
    }

    function onDragMoveEnd(dragState, force, graph, d) {
        d.fixed &= ~6; // copied from force.drag
        if (dragState.isDragging) {
            // if we were dragging before, the node should be fixed
            setFixed(graph, d);
            force.alpha(0);
        } else {
            // if the user just clicked, the position should be reset.
            unsetFixed(graph, d);
            force.resume();
        }

        dragState.isDragging = false;

        d.d3NodeContainer.classed({
            "moving": false
        });
    }

    function disconnectHyperRelation(graph, d) {
        let start = Post.$buildRaw({id: d.startId});
        start.connectsTo.$buildRaw({id: d.endId}).$destroy().$then(response => {
            graph.removeNode(d);
            graph.commit();
        });
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

    function setInitialNodePositions(globalState, graph) {
        let squareFactor = 100 * Math.sqrt(graph.nodes.length);
        _(graph.nonHyperRelationNodes).filter(n => isNaN(n.x) || isNaN(n.y)).each(n => {
            let hash = Math.abs(Helpers.hashCode(n.id));
            n.x = squareFactor * (hash & 0xfff) / 0xfff + globalState.width / 2 - squareFactor / 2;
            n.y = squareFactor * n.verticalForce / graph.nonHyperRelationNodes.length + globalState.height / 2 - squareFactor / 2;
        }).value();

        _(graph.hyperRelations).filter(n => isNaN(n.x) || isNaN(n.y)).each(n => {
            n.x = (n.source.x + n.target.x) / 2;
            n.y = (n.source.y + n.target.y) / 2;
        }).value();
    }

    function calculateNodeVerticalForce(graph) {
        // bring nodes in order by calculating the difference between following and
        // leading nodes. Then assign numbers from -(nodes.length/2) to +(nodes.length/2).
        // This is used as force to pull nodes upwards or downwards.
        _(graph.nonHyperRelationNodes).each(node => {
            let deepReplies = node.deepSuccessors.length - node.deepPredecessors.length;
            node.verticalForce = deepReplies;
        }).sortBy("verticalForce").each((n, i) => n.verticalForce = i).value();
    }

    function converge(globalState, force, graph, zoom, d3HtmlContainer, d3SvgContainer, onDraw, transformCompat) {
        let convergeIterations = 0;
        initConverge(globalState, graph, d3HtmlContainer, d3SvgContainer, zoom);

        if (globalState.visibleConvergence) {
            //TODO: why two times afterConverge? also in nonBlockingConverge
            force.on("end", _.once(_.partial(afterConverge, globalState, graph, d3HtmlContainer, d3SvgContainer, zoom, onDraw, convergeIterations, transformCompat))); // we don't know how to unsubscribe
        } else {
            requestAnimationFrame(nonBlockingConverge);
        }


        function nonBlockingConverge() {
            let startTime = Date.now();
            // keep a constant frame rate
            while (((startTime + 300) > Date.now()) && (force.alpha() > 0)) {
                force.tick();
                convergeIterations++;
            }

            if (force.alpha() > 0) {
                requestAnimationFrame(nonBlockingConverge);
            } else {
                afterConverge(globalState, graph, d3HtmlContainer, d3SvgContainer, zoom, onDraw, convergeIterations, transformCompat);
            }
        }
    }

    function initConverge(globalState, graph, d3HtmlContainer, d3SvgContainer, zoom, transformCompat) {
        // focusMarkedNodes needs visible/marked nodes and edges
        _.each(graph.nodes, n => {
            n.marked = true;
            n.visible = true;
        });
        _.each(graph.edges, e => {
            e.visible = true;
        });

        if (globalState.visibleConvergence) {
            recalculateNodeDimensions(graph);
            focusMarkedNodes(graph, zoom, globalState.width, globalState.height, d3HtmlContainer, d3SvgContainer, transformCompat, 0);
            d3HtmlContainer.style("visibility", "visible");
            d3SvgContainer.style("visibility", "visible");
        }
    }

    function afterConverge(globalState, graph, d3HtmlContainer, d3SvgContainer, zoom, onDraw, convergeIterations, transformCompat) {
        setFixed(graph, graph.rootNode);

        globalState.drawOnTick = true;
        onDraw();
        if (globalState.visibleConvergence)
            focusMarkedNodes(graph, zoom, globalState.width, globalState.height, d3HtmlContainer, d3SvgContainer, transformCompat);
        else
            focusMarkedNodes(graph, zoom, globalState.width, globalState.height, d3HtmlContainer, d3SvgContainer, transformCompat, 0);


        d3HtmlContainer.style("visibility", "visible");
        d3SvgContainer.style("visibility", "visible");
    }

    function updateGraphRefs(graph, d3NodeContainer, d3Node, d3NodeTools, d3LinkPath) {
        // write dom element ref and rect into graph node
        // for easy lookup
        _.each(graph.nodes, (n, i) => {
            n.domNodeContainer = d3NodeContainer[0][i];
            n.d3NodeContainer = d3.select(n.domNodeContainer);

            n.domNode = d3Node[0][i];
            n.d3Node = d3.select(n.domNode);

            n.domNodeTools = d3NodeTools[0][i];
            n.d3NodeTools = d3.select(n.domNodeTools);
        });

        _.each(graph.edges, (r, i) => {
            r.domPath = d3LinkPath[0][i];
            r.d3Path = d3.select(r.domPath);
        });
    }

    function recalculateNodeDimensions(graph) {
        _.each(graph.nodes, n => {
            n.rect = {
                width: n.domNode.offsetWidth,
                height: n.domNode.offsetHeight
            };
        });
    }

    // zoom into graph
    function zoomed(d3SvgContainer, d3HtmlContainer, transformCompat) {
        applyZoom(d3.event.translate, d3.event.scale, d3SvgContainer, d3HtmlContainer, transformCompat);
    }

    function applyZoom(translate, scale, d3SvgContainer, d3HtmlContainer, transformCompat) {
        d3SvgContainer.attr("transform", "translate(" + translate[0] + ", " + translate[1] + ") scale(" + scale + ")");
        d3HtmlContainer.style(transformCompat, "translate(" + translate[0] + "px, " + translate[1] + "px) scale(" + scale + ")");
    }

    // unfix the position of a given node
    function unsetFixed(graph, d) {
        d.fixed = false;
        d.d3NodeContainer.classed({
            "fixed": false
        });

        // the fixed class could change the elements dimensions
        recalculateNodeDimensions(graph);
    }

    // fix the position of a given node
    function setFixed(graph, d) {
        d.fixed = true;
        d.d3NodeContainer.classed({
            "fixed": true
        });

        // the fixed class could change the elements dimensions
        recalculateNodeDimensions(graph);
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


    // filter the graph
    function filter(globalState, graph, zoom, d3SvgContainer, d3HtmlContainer, event, filtered) {
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

        setVisibility(graph);
        focusMarkedNodes(graph, zoom, globalState.width, globalState.height, d3HtmlContainer, d3SvgContainer);
    }

    // reset visibility of nodes after filtering
    function setVisibility(graph) {
        let notMarkedOpacity = 0.3;
        // set node visibility
        _.each(graph.nodes, node => {
            let opacity = (node.marked) ? 1.0 : notMarkedOpacity;
            let visibility = node.visible ? "inherit" : "hidden";
            node.domNode.style.opacity = opacity;
            node.domNode.style.visibility = visibility;
            node.domNodeTools.style.opacity = opacity;
            node.domNodeTools.style.visibility = node.hyperEdge ? "hidden" : visibility;
        });

        // set edge visibility
        _.each(graph.edges, (edge, i) => {
            edge.domPath.style.visibility = edge.visible ? "inherit" : "hidden";
            edge.domPath.style.opacity = (edge.source.marked === true && edge.target.marked === true) ? 1.0 : notMarkedOpacity;
        });
    }

    // focus the marked nodes and scale zoom accordingly
    function focusMarkedNodes(graph, zoom, width, height, d3HtmlContainer, d3SvgContainer, transformCompat, duration = 500) {
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
            d3HtmlContainer.transition().duration(duration).call(zoom.translate(translate).scale(scale).event);
            d3SvgContainer.transition().duration(duration).call(zoom.translate(translate).scale(scale).event);
        } else {
            // skip animation if duration is zero
            d3HtmlContainer.call(zoom.translate(translate).scale(scale).event);
            d3SvgContainer.call(zoom.translate(translate).scale(scale).event);
        }

        drawGraph(graph, transformCompat);
    }

    // resize graph according to the current element dimensions
    function resizeGraph(graph, globalState, zoom, rootDomElement, d3SvgContainer, d3HtmlContainer, d3NodeContainer, transformCompat) {
        globalState.width = rootDomElement.offsetWidth;
        globalState.height = rootDomElement.offsetHeight;
        let [width, height] = [globalState.width, globalState.height];
        d3SvgContainer.style("width", width).style("height", height);
        d3HtmlContainer.style("width", width + "px").style("height", height + "px");
        // if graph was hidden when initialized,
        // all foreign objects have size 0
        // this call recalculates the sizes
        focusMarkedNodes(graph, zoom, width, height, d3HtmlContainer, d3SvgContainer, d3NodeContainer, transformCompat);
        recalculateNodeDimensions(graph);
    }

    // tick function, called in each step in the force calculation,
    // maps elements to positions
    function tick(graph, globalState, transformCompat, e) {
        // push hypernodes towards the center between its start/end node
        let pullStrength = e.alpha * globalState.hyperRelationAlignForce;
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
                node.x += (center.x - node.x) * pullStrength;
                node.y += (center.y - node.y) * pullStrength;
                let newStartDiffX = start.x - node.x;
                let newStartDiffY = start.y - node.y;
                let newEndDiffX = end.x - node.x;
                let newEndDiffY = end.y - node.y;
                if (start.fixed !== true) {
                    start.x += (startDiffX - newStartDiffX) * pullStrength;
                    start.y += (startDiffY - newStartDiffY) * pullStrength;
                }
                if (end.fixed !== true) {
                    end.x += (endDiffX - newEndDiffX) * pullStrength;
                    end.y += (endDiffY - newEndDiffY) * pullStrength;
                }
            }
        });

        // pull nodes with more more children up
        graph.nonHyperRelationNodes.forEach(node => {
            if (node.fixed !== true) {
                node.y += (node.verticalForce - graph.nonHyperRelationNodes.length / 2) * e.alpha * globalState.nodeVerticalForce;
            }
        });

        if (globalState.drawOnTick) {
            drawGraph(graph, transformCompat);
        }
    }

    function drawGraph(graph, transformCompat) {
        _.each(graph.nodes, (node) => {
            node.domNodeContainer.style[transformCompat] = "translate(" + (node.x - node.rect.width / 2) + "px," + (node.y - node.rect.height / 2) + "px)";
        });

        _.each(graph.edges, (relation) => {
            // draw svg paths for lines between nodes
            if (relation.source.id === relation.target.id) { // self loop
                //TODO: self loops with hypernodes
                let rect = relation.rect;
                relation.domPath.setAttribute("d", `
                        M ${relation.source.x} ${relation.source.y - rect.height/2}
                        m -20, 0
                        c -80,-80   120,-80   40,0
                        `);
            } else {
                // clamp every edge line to the intersections with its incident node rectangles
                let line = Helpers.clampLineByRects(relation, relation.source.rect, relation.target.rect);
                let pathAttr = `M ${line.x1} ${line.y1} L ${line.x2} ${line.y2}`;
                relation.domPath.setAttribute("d", pathAttr);
            }


            // draw normal link-labels and center them
            // let domLinkTextNode = domLinks[i];
            // let rect = relation.rect;
            // if (relation.source.id === relation.target.id) { // self loop
            //     domLinkTextNode.style[transformCompat] = "translate(" + (relation.source.x - rect.width / 2) + "px," + (relation.source.y - rect.height / 2 - 70) + "px)";
            // } else {
            //     domLinkTextNode.style[transformCompat] = "translate(" + (((relation.source.x + relation.target.x) / 2) - rect.width / 2) + "px," + (((relation.source.y + relation.target.y) / 2) - rect.height / 2) + "px)";
            // }
        });
    }

}
