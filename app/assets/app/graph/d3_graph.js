angular.module("wust.graph").directive("d3Graph", d3Graph);

d3Graph.$inject = ["$window", "DiscourseNode", "Helpers", "$location", "$filter", "Post", "$compile"];

function d3Graph($window, DiscourseNode, Helpers, $location, $filter, Post, $compile) {

    function link(scope, element) {

        class D3Graph {
            //TODO: rename onClick -> onNodeClick
            //TODO: rename edge, link, ... -> relation
            constructor(graph, rootDomElement, onClick = _.noop, onDraw = _.noop) {
                this.graph = graph;
                this.rootDomElement = rootDomElement;
                this.onClick = onClick;
                this.onDraw = onDraw;

                // settings
                this.visibleConvergence = false;
                this.debugDraw = false;
                this.hyperRelationAlignForce = 0.5;
                this.nodeVerticalForce = 1;
                // state
                this.drawOnTick = this.drawOnTick = this.visibleConvergence;
                this.hoveredNode = undefined;
                this.width = rootDomElement.offsetWidth;
                this.height = rootDomElement.offsetHeight;

                // state for drag+drop
                this.isDragging = false;
                this.dragStartNode = undefined;
                this.dragStartNodeX = undefined;
                this.dragStartNodeY = undefined;
                this.dragStartMouseX = undefined;
                this.dragStartMouseY = undefined;
                this.dragStartOffsetX = undefined;
                this.dragStartOffsetY = undefined;

                this.force = d3.layout.force()
                    .size([this.width, this.height])
                    .nodes(graph.nodes)
                    .links(graph.edges)
                    .linkStrength(0.9) // rigidity
                    .friction(0.92)
                    .linkDistance(100) // weak geometric constraint. Pushes nodes to achieve this distance
                    .charge(-1300)
                    .chargeDistance(1000)
                    .gravity(0.01)
                    .theta(0.8)
                    .alpha(0.1);
                this.zoom = d3.behavior.zoom().scaleExtent([0.1, 10]); // min/max zoom level

            }

            init() {
                this.initDom();

                // call tick on every simulation step
                this.force.on("tick", this.tick.bind(this));
                this.graph.onCommit(this.updateGraph.bind(this));

                this.updateGraph();

                this.converge();
            }

            initDom() {
                // svg will stay in background and only render the edges
                this.d3Svg = d3.select(this.rootDomElement)
                    .append("svg")
                    .attr("width", this.width)
                    .attr("height", this.height)
                    .style("position", "absolute");
                // .style("background", "rgba(220, 255, 240, 0.5)");

                // has the same size and position as the svg
                // renders nodes and relation labels
                this.d3Html = d3.select(this.rootDomElement)
                    .append("div")
                    .style("width", this.width + "px")
                    .style("height", this.height + "px")
                    .style("position", "absolute");
                // .style("background", "rgba(220, 240, 255, 0.5)");
                // .style("border", "1px solid #333")

                // contains markers for arrows
                this.d3SvgDefs = this.d3Svg.append("svg:defs");
                // svg-marker for relation arrows
                this.d3SvgDefs.append("svg:marker")
                    .attr("id", "graph_arrow")
                    .attr("viewBox", "0 -3 10 6")
                    .attr("refX", 10)
                    .attr("markerWidth", 15)
                    .attr("markerHeight", 9)
                    .attr("orient", "auto")
                    .append("svg:path")
                    .attr("d", "M 0,-3 L 10,-0.5 L 10,0.5 L0,3")
                    .attr("class", "svglink"); // for the stroke color

                // svg-marker for connector line arrow
                this.d3SvgDefs.append("svg:marker")
                    .attr("id", "graph_connector_arrow")
                    .attr("viewBox", "-5 -1.5 5 3")
                    .attr("refX", -5)
                    .attr("markerWidth", 5)
                    .attr("markerHeight", 3)
                    .attr("orient", "auto")
                    .append("svg:path")
                    .attr("d", "M 0,-1.5 L -5,-0.5 L -5,0.5 L0,1.5")
                    .attr("class", "connectorlinearrow"); // for the stroke color

                // choose the correct transform style for many browsers
                this.transformCompat = Helpers.cssCompat("transform", "Transform", "transform");
                this.transformOriginCompat = Helpers.cssCompat("transformOrigin", "TransformOrigin", "transform-origin");


                // svg and html each have full-size
                // containers with enabled pointer events
                // translate-styles for zoom/pan will be applied here
                this.d3SvgContainer = this.d3Svg.append("g").attr("id", "svgContainer")
                    .style("visibility", "hidden"); // will be shown when converged
                this.d3HtmlContainer = this.d3Html.append("div").attr("id", "htmlContainer")
                    // zoom fix: html initially has its origin centered, svg has (top left)
                    .style(this.transformOriginCompat, "top left")
                    .style("visibility", "hidden"); // will be shown when converged

                if (this.debugDraw) {
                    // draw gravitational center
                    this.d3SvgContainer.append("circle")
                        .attr("cx", this.width / 2)
                        .attr("cy", this.height / 2)
                        .attr("r", 20)
                        .style("fill", "#7B00D6");

                    // draw origin
                    this.d3SvgContainer.append("circle")
                        .attr("cx", 0)
                        .attr("cy", 0)
                        .attr("r", 20);
                }

                // contains all nodes
                this.d3NodeContainer = this.d3HtmlContainer.append("div")
                    .attr("id", "hypernodes-then-nodes")
                    .attr("class", "nodecontainer");

                // contains all relations
                this.d3LinkPath = this.d3SvgContainer.append("g").attr("id", "linkContainer");

                this.d3ConnectorLine = this.d3SvgContainer.append("line").classed({
                    "connectorline": true
                }).style("marker-start", "url(" + $location.absUrl() + "#graph_connector_arrow)");

                // add use element as the last(!) element to the svg, this controls the foremost element
                //http://stackoverflow.com/a/6289809
                // this.svgUseElement = this.d3NodeContainer.append("use").attr("id", "svgUseElement").attr("xlink:href", "#svgUseElement");
            }


            updateGraph(changes) {
                //add nodes to svg, first hypernodes then nodes, so the normal
                //nodes are drawn on top of the hypernodes in the svg
                this.graph.setNodes(_.sortBy(this.graph.nodes, n => !n.hyperEdge));

                this.calculateNodeVerticalForce();
                this.setInitialNodePositions();

                // console.log("------ update graph");
                // console.log(graph.nonHyperRelationNodes.map((n) => n.title), graph.hyperRelations.map((r) => r.source.title + " --> " + r.target.title));
                // create data joins
                // http://bost.ocks.org/mike/join/
                this.d3NodeContainerWithData = this.d3NodeContainer
                    .selectAll("div")
                    .data(this.graph.nodes, (d) => d.id);

                this.d3LinkPathWithData = this.d3LinkPath
                    .selectAll("path")
                    .data(this.graph.edges, (d) => d.startId + " --> " + d.endId);

                // add nodes
                let d3NodeFrame = this.d3NodeContainerWithData.enter()
                    .append("div").attr("class","nodeframe")
                    .style("position", "relative") // needed for z-index (moving/fixed have higher z-index)
                    .style("pointer-events", "all");

                this.d3Node = this.d3NodeContainerWithData.append("div")
                    .attr("class", d => d.hyperEdge ? "no_flick relation_label" : `no_flick node ${DiscourseNode.get(d.label).css}`)
                    .style("position", "absolute")
                    .style("max-width", "150px") // to produce line breaks
                    .style("word-wrap", "break-word")
                    .style("cursor", "pointer");

                this.d3Node
                    .append("span")
                    .text(d => d.title);
                    // .style("border-width", n => Math.abs(n.verticalForce) + "px")
                    // .style("border-color", n => n.verticalForce < 0 ? "#3CBAFF" : "#FFA73C")

                // add relations
                this.d3LinkPathWithData.enter()
                    .append("path")
                    .attr("class", "svglink")
                    .each(function(link) {
                        // if link is startRelation of a Hypernode
                        if (!(link.target.hyperEdge && link.target.startId === link.source.id)) {
                            d3.select(this).style("marker-end", "url(" + $location.absUrl() + "#graph_arrow)");
                        }
                    });

                this.d3NodeTools = this.d3NodeContainerWithData.append("div")
                    .attr("class", "nodetools");

                this.d3NodePinTool = this.d3NodeTools.append("div")
                    .attr("class", "nodetool pintool fa fa-thumb-tack fa-rotate-45")
                    .style("cursor", "pointer");

                this.d3NodeConnectTool = this.d3NodeTools.append("div")
                    .style("display", d => d.hyperEdge ? "none" : "inline-block")
                    .attr("class", "nodetool connecttool icon-flow-line fa-rotate-minus45")
                    .style("cursor", "crosshair");

                this.d3NodeDisconnectTool = this.d3NodeTools.append("div")
                    .style("display", d => d.hyperEdge ? "inline-block" : "none")
                    .attr("class", "nodetool disconnecttool fa fa-scissors")
                    .style("cursor", "pointer");

                this.d3NodeDeleteTool = this.d3NodeTools.append("div")
                    .style("display", d => d.hyperEdge ? "none" : "inline-block")
                    .attr("class", "nodetool deletetool fa fa-trash")
                    .style("cursor", "pointer");

                /// remove nodes and relations
                this.d3NodeContainerWithData.exit().remove();
                this.d3LinkPathWithData.exit().remove(); //

                // console.log(graph.nodes.map(n => n.id.slice(0,3)),d3NodeContainer.node());
                // console.log(graph.edges,d3LinkPath.node());

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

                this.updateGraphRefs();
                this.recalculateNodeDimensions();

                // now we can use the calculated rect
                this.d3Node
                    .attr("description-popover", "")
                    .attr("node-id", d => d.id)
                    .attr("enable-position-hack", true);

                this.registerUIEvents();
                this.force.nodes(this.graph.nodes); // nodes and edges get replaced instead of just changed by scalajs
                this.force.links(this.graph.edges); // that's why we need to set the new references
                // force.tick();
                // drawGraph(graph, transformCompat);
                this.force.start();

                this.registerUIEvents();

                $compile(this.d3Html[0])(scope);
            }

            registerUIEvents() {
                function stopPropagationAfter(func) {
                    return d => {
                    console.log(d3.event);
                        d3.event.stopImmediatePropagation();
                        func(d);
                    };
                }

                // from: http://stackoverflow.com/a/17111220/793909
                let dragInitiated = false;
                function dragStartWithButton(button, func) {
                    return d => {
                        if(d3.event.sourceEvent.which === button) {
                            dragInitiated = true;
                            func(d);
                        }
                        d3.event.sourceEvent.stopPropagation();
                    };
                }
                function dragWithButton(button, func) {
                    return d => {
                        if(d3.event.sourceEvent.which === button && dragInitiated) {
                            func(d);
                        }
                        d3.event.sourceEvent.stopPropagation();
                    };
                }
                function dragEndWithButton(button, func) {
                    return d => {
                        if(d3.event.sourceEvent.which === button && dragInitiated) {
                            func(d);
                            dragInitiated = false;
                        }
                        d3.event.sourceEvent.stopPropagation();
                    };
                }

                // define events
                this.zoom.on("zoom", this.zoomed.bind(this));

                let dragMove = d3.behavior.drag()
                    .on("dragstart", dragStartWithButton(1, this.onDragMoveStart.bind(this)))
                    .on("drag", dragWithButton(1, this.onDragMove.bind(this)))
                    .on("dragend", dragEndWithButton(1, this.onDragMoveEnd.bind(this)));

                let dragConnect = d3.behavior.drag()
                    .on("dragstart", dragStartWithButton(1, this.onDragConnectStart.bind(this)))
                    .on("drag", dragWithButton(1, this.onDragConnectMove.bind(this)))
                    .on("dragend", dragEndWithButton(1, this.onDragConnectEnd.bind(this)));

                let disableDrag = d3.behavior.drag()
                    .on("dragstart", () => d3.event.sourceEvent.stopPropagation());

                this.d3Html.call(this.zoom)
                    .on("dblclick.zoom", null)
                     .on("mousedown", () => {
                         this.force.stop();
                     })
                     .on("dblclick", () => {
                        this.force.resume();
                     });

                this.d3Svg.on("dblclick.zoom", null);

                this.d3Node/*.on("click", this.ignoreHyperEdge(node => {
                        this.onClick({
                            node
                        });
                    }))*/
                    .on("mouseover", d => this.hoveredNode = d)
                    .on("mouseout", d => {
                        this.hoveredNode = undefined;
                        d.d3NodeContainer.classed({
                            "selected": false
                        });
                    });

                this.d3Node.call(dragMove);
                this.d3NodePinTool.on("click", stopPropagationAfter(this.toggleFixed.bind(this))).call(disableDrag);
                this.d3NodeConnectTool.call(dragConnect);
                this.d3NodeDisconnectTool.on("click", stopPropagationAfter(this.disconnectHyperRelation.bind(this))).call(disableDrag);
                this.d3NodeDeleteTool.on("click", stopPropagationAfter(this.removeNode.bind(this))).call(disableDrag);

                // register for resize event
                angular.element($window).bind("resize", this.resizeGraph.bind(this));
            }


            // executes specified function only for normal nodes, i.e.,
            // ignores hyperedges
            ignoreHyperEdge(func) {
                return d => {
                    // do nothing for hyperedges
                    if (d.hyperEdge)
                        return;

                    func(d);
                };
            }

            setInitialNodePositions() {
                let squareFactor = 100 * Math.sqrt(this.graph.nodes.length);
                _(this.graph.nonHyperRelationNodes).filter(n => isNaN(n.x) || isNaN(n.y)).each(n => {
                    let hash = Math.abs(Helpers.hashCode(n.id));
                    n.x = squareFactor * (hash & 0xfff) / 0xfff + this.width / 2 - squareFactor / 2;
                    n.y = squareFactor * n.verticalForce / this.graph.nonHyperRelationNodes.length + this.height / 2 - squareFactor / 2;
                }).value();

                _(this.graph.hyperRelations).filter(n => isNaN(n.x) || isNaN(n.y)).each(n => {
                    n.x = (n.source.x + n.target.x) / 2;
                    n.y = (n.source.y + n.target.y) / 2;
                }).value();
            }

            calculateNodeVerticalForce() {
                // bring nodes in order by calculating the difference between following and
                // leading nodes. Then assign numbers from -(nodes.length/2) to +(nodes.length/2).
                // This is used as force to pull nodes upwards or downwards.
                _(this.graph.nonHyperRelationNodes).each(node => {
                    let deepReplies = node.deepSuccessors.length - node.deepPredecessors.length;
                    node.verticalForce = deepReplies;
                }).sortBy("verticalForce").each((n, i) => n.verticalForce = i).value();
            }

            converge() {
                // let convergeIterations = 0;
                this.initConverge();

                if (this.visibleConvergence) {
                    //TODO: why two times afterConverge? also in nonBlockingConverge
                    this.force.on("end", _.once(this.afterConverge.bind(this))); // we don't know how to unsubscribe
                } else {
                    requestAnimationFrame(this.nonBlockingConverge.bind(this));
                }


            }

            nonBlockingConverge() {
                let startTime = Date.now();
                // keep a constant frame rate
                while (((startTime + 300) > Date.now()) && (this.force.alpha() > 0)) {
                    this.force.tick();
                    // convergeIterations++;
                }

                if (this.force.alpha() > 0) {
                    requestAnimationFrame(this.nonBlockingConverge.bind(this));
                } else {
                    this.afterConverge();
                }
            }

            initConverge() {
                // focusMarkedNodes needs visible/marked nodes and edges
                this.graph.nodes.forEach( n => {
                    n.marked = true;
                    n.visible = true;
                });
                this.graph.edges.forEach( e => {
                    e.visible = true;
                });
                if (this.visibleConvergence) {
                    this.recalculateNodeDimensions();
                    this.focusMarkedNodes(0);
                    this.d3HtmlContainer.style("visibility", "visible");
                    this.d3SvgContainer.style("visibility", "visible");
                }

            }

            afterConverge() {
                this.resizeGraph();

                this.setFixed(this.graph.rootNode);

                this.drawOnTick = true;
                this.onDraw();
                if (this.visibleConvergence)
                    this.focusMarkedNodes();
                else
                    this.focusMarkedNodes(0);


                this.d3HtmlContainer.style("visibility", "visible");
                this.d3SvgContainer.style("visibility", "visible");
            }

            updateGraphRefs() {
                // write dom element ref and rect into graph node
                // for easy lookup
                this.graph.nodes.forEach((n, i) => {
                    n.domNodeContainer = this.d3NodeContainerWithData[0][i];
                    n.d3NodeContainer = d3.select(n.domNodeContainer);

                    n.domNode = this.d3Node[0][i];
                    n.d3Node = d3.select(n.domNode);

                    n.domNodeTools = this.d3NodeTools[0][i];
                    n.d3NodeTools = d3.select(n.domNodeTools);
                });

                this.graph.edges.forEach( (r, i) => {
                    r.domPath = this.d3LinkPathWithData[0][i];
                    r.d3Path = d3.select(r.domPath);
                });
            }

            recalculateNodeDimensions() {
                this.graph.nodes.forEach( n => {
                    n.rect = {
                        width: n.domNode.offsetWidth,
                        height: n.domNode.offsetHeight
                    };
                });
            }

            // tick function, called in each step in the force calculation,
            // maps elements to positions
            tick(e) {
                // push hypernodes towards the center between its start/end node
                let pullStrength = e.alpha * this.hyperRelationAlignForce;
                this.graph.hyperRelations.forEach(node => {
                    if (!node.fixed) {
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
                this.graph.nonHyperRelationNodes.forEach(node => {
                    if (node.fixed !== true) {
                        node.y += (node.verticalForce - this.graph.nonHyperRelationNodes.length / 2) * e.alpha * this.nodeVerticalForce;
                    }
                });

                if (this.drawOnTick)
                    this.drawGraph();
            }

            drawGraph() {
                this.graph.nodes.forEach( (node) => {
                    node.domNodeContainer.style[this.transformCompat] = "translate(" + (node.x - node.rect.width / 2) + "px," + (node.y - node.rect.height / 2) + "px)";
                });

                this.graph.edges.forEach( (relation) => {
                    // draw svg paths for lines between nodes
                    // if (relation.source.id === relation.target.id) { // self loop
                    //     //TODO: self loops with hypernodes
                    //     let rect = relation.rect;
                    //     relation.domPath.setAttribute("d", `
                    //             M ${relation.source.x} ${relation.source.y - rect.height/2}
                    //             m -20, 0
                    //             c -80,-80   120,-80   40,0
                    //             `);
                    // } else {
                    // clamp every edge line to the intersections with its incident node rectangles
                    let line = Helpers.clampLineByRects(relation, relation.source.rect, relation.target.rect);
                    if (isNaN(line.x1) || isNaN(line.y1) || isNaN(line.x2) || isNaN(line.y2))
                        console.warn("invalid coordinates for relation");
                    else {
                        let pathAttr = `M ${line.x1} ${line.y1} L ${line.x2} ${line.y2}`;
                        relation.domPath.setAttribute("d", pathAttr);
                    }
                    // }


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

            // zoom into graph
            zoomed() {
                this.applyZoom(d3.event.translate, d3.event.scale);
            }

            applyZoom(translate, scale) {
                this.d3SvgContainer.attr("transform", "translate(" + translate[0] + ", " + translate[1] + ") scale(" + scale + ")");
                this.d3HtmlContainer.style(this.transformCompat, "translate(" + translate[0] + "px, " + translate[1] + "px) scale(" + scale + ")");
            }

            // resize graph according to the current element dimensions
            resizeGraph() {
                this.width = this.rootDomElement.offsetWidth;
                this.height = this.rootDomElement.offsetHeight;
                let [width, height] = [this.width, this.height];
                this.d3Svg.style("width", width).style("height", height);
                this.d3Html.style("width", width + "px").style("height", height + "px");
                // if graph was hidden when initialized,
                // all foreign objects have size 0
                // this call recalculates the sizes
                this.focusMarkedNodes();
                this.recalculateNodeDimensions();
            }


            // focus the marked nodes and scale zoom accordingly
            focusMarkedNodes(duration = 500) {
                if (this.width === 0 || this.height === 0) return;
                let marked = _.select(this.graph.nodes, {
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
                    scale = Math.min(1, 0.9 * this.width / (max[0] - min[0]), 0.9 * this.height / (max[1] - min[1]));
                }

                let translate = [this.width / 2 - center[0] * scale, this.height / 2 - center[1] * scale];

                if (duration > 0) {
                    this.d3HtmlContainer.transition().duration(duration).call(this.zoom.translate(translate).scale(scale).event);
                    this.d3SvgContainer.transition().duration(duration).call(this.zoom.translate(translate).scale(scale).event);
                } else {
                    // skip animation if duration is zero
                    this.d3HtmlContainer.call(this.zoom.translate(translate).scale(scale).event);
                    this.d3SvgContainer.call(this.zoom.translate(translate).scale(scale).event);
                }

                this.drawGraph();
            }


            // filter the graph
            filter(matchingNodes) {
                let component = _(matchingNodes).map(node => node.component).flatten().uniq().value();

                this.graph.nodes.forEach( node => {
                    node.marked = _(matchingNodes).contains(node);
                    node.visible = node.marked || _(component).contains(node);

                });

                this.graph.nodes.forEach( node => {
                    if (node.hyperEdge) {
                        //TODO: mark chains of hyperedges
                        node.marked = node.marked || node.source.marked && node.target.marked;
                    }
                });

                this.graph.edges.forEach( edge => {
                    edge.visible = _(component).contains(edge.source) && _(component).contains(edge.target);
                });

                this.setVisibility();
                this.focusMarkedNodes();
            }

            // reset visibility of nodes after filtering
            setVisibility() {
                let notMarkedOpacity = 0.3;
                // set node visibility
                this.graph.nodes.forEach( node => {
                    let opacity = (node.marked) ? 1.0 : notMarkedOpacity;
                    let visibility = node.visible ? "inherit" : "hidden";
                    node.domNode.style.opacity = opacity;
                    node.domNode.style.visibility = visibility;
                    node.domNodeTools.style.opacity = opacity;
                    node.domNodeTools.style.visibility = visibility;
                });

                // set edge visibility
                this.graph.edges.forEach( (edge, i) => {
                    edge.domPath.style.visibility = edge.visible ? "inherit" : "hidden";
                    edge.domPath.style.opacity = (edge.source.marked === true && edge.target.marked === true) ? 1.0 : notMarkedOpacity;
                });
            }

            toggleFixed(d) {
                if (d.fixed) this.unsetFixed(d);
                else this.setFixed(d);
            }

            // fix the position of a given node
            setFixed(d) {
                d.fixed = true;
                d.d3NodeContainer.classed({
                    "fixed": true
                });

                // the fixed class could change the elements dimensions
                this.recalculateNodeDimensions();
                this.force.alpha(0);
            }

            // unfix the position of a given node
            unsetFixed(d) {
                d.fixed = false;
                d.d3NodeContainer.classed({
                    "fixed": false
                });

                // the fixed class could change the elements dimensions
                this.recalculateNodeDimensions();
                this.force.resume();
            }

            disconnectHyperRelation(d) {
                Post.$buildRaw({
                    id: d.startId
                }).connectsTo.$buildRaw({
                    id: d.endId
                }).$destroy().$then(response => {
                }, response => humane.error("Server error:\n" + response));
                this.graph.removeNode(d.id);
                this.graph.commit();
                this.force.stop();
            }

            removeNode(d) {
                Post.$buildRaw({
                    id: d.id
                }).$destroy().$then(response => {
                }, response => humane.error("Server error:\n" + response));
                this.graph.removeNode(d.id);
                this.graph.commit();
                this.force.stop();
            }


            setNodePositionFromOffset(node, x, y) {
                let scale = this.zoom.scale();
                let translate = this.zoom.translate();
                node.x = (x - translate[0]) / scale;
                node.y = (y - translate[1]) / scale;
                node.px = node.x;
                node.py = node.y;
            }

            onDragStartInit(d) {
                // prevent d3 from interpreting this as panning
                d3.event.sourceEvent.stopPropagation();

                let event = d3.event.sourceEvent;
                let scale = this.zoom.scale();
                var target = event.target || event.srcElement;

                this.dragStartNodeX = d.x;
                this.dragStartNodeY = d.y;
                this.dragStartMouseX = event.clientX;
                this.dragStartMouseY = event.clientY;
                this.dragStartNode = d;

                let domRect = d.domNode.getBoundingClientRect();
                let eventRect = target.getBoundingClientRect();
                this.dragOffsetX = (eventRect.left - domRect.left) / scale + event.offsetX - d.domNode.offsetWidth / 2;
                this.dragOffsetY = (eventRect.top - domRect.top) / scale + event.offsetY - d.domNode.offsetHeight / 2;
            }

            //TODO: rename d to something meaningful in all d3 code
            onDragMoveStart(d) {
                this.onDragStartInit(d);

                d.fixed |= 2; // copied from force.drag
            }

            onDragConnectStart(d) {
                this.onDragStartInit(d);

                this.d3ConnectorLine
                    .attr("x1", this.dragStartNodeX)
                    .attr("y1", this.dragStartNodeY)
                    .attr("x2", this.dragStartNodeX)
                    .attr("y2", this.dragStartNodeY)
                    .classed({
                        "moving": true
                    });

                this.force.alpha(0);
            }

            onDragMoveInit(d, onStartDragging = () => {}) {
                // check whether there was a substantial mouse movement. if
                // not, we will interpret this as a click event after the
                // mouse button is released (see onDragMoveEnd handler).
                let event = d3.event.sourceEvent;
                let diffX = this.dragStartMouseX - event.clientX;
                let diffY = this.dragStartMouseY - event.clientY;
                let diff = Math.sqrt(diffX * diffX + diffY * diffY);
                if (!this.isDragging) {
                    if (diff > 5) {
                        this.isDragging = true;
                        onStartDragging();
                    }
                }
            }

            onDragMove(d) {
                //TODO: fails when zooming/scrolling and dragging at the same time
                this.onDragMoveInit(d, () => d.d3NodeContainer.classed({
                    "moving": true
                }));

                if (this.isDragging) {
                    // default positioning is center of node.
                    // but we let node stay under grabbed position.
                    let event = d3.event.sourceEvent;
                    let scale = this.zoom.scale();
                    d.px = this.dragStartNodeX + (event.clientX - this.dragStartMouseX) / scale;
                    d.py = this.dragStartNodeY + (event.clientY - this.dragStartMouseY) / scale;
                    this.force.resume(); // restart annealing
                }
            }

            onDragConnectMove(d) {
                //TODO: fails when zooming/scrolling and dragging at the same time
                let event = d3.event.sourceEvent;
                let scale = this.zoom.scale();

                this.onDragMoveInit(d);

                if (this.isDragging) {
                    // default positioning is center of node.
                    // but we let node stay under grabbed position.
                    this.d3ConnectorLine
                        .attr("x1", this.dragStartNodeX + this.dragOffsetX + (event.clientX - this.dragStartMouseX) / scale)
                        .attr("y1", this.dragStartNodeY + this.dragOffsetY + (event.clientY - this.dragStartMouseY) / scale);

                    if (this.hoveredNode !== undefined) {
                        this.hoveredNode.d3NodeContainer.classed({
                            "selected": true
                        });
                        this.dragStartNode.d3NodeContainer.classed({
                            "selected": true
                        });
                    } else {
                        this.dragStartNode.d3NodeContainer.classed({
                            "selected": false
                        });
                    }
                }
            }

            // we use dragend instead of click event, because it is emitted on mobile phones as well as on pcs
            onDragConnectEnd() {
                if (this.isDragging) {
                    if (this.hoveredNode !== undefined) {
                        let sourceNode = this.dragStartNode; // always normal node
                        let targetNode = this.hoveredNode;
                        let referenceNode;
                        if (targetNode.hyperEdge) {
                            let start = Post.$buildRaw({
                                id: targetNode.startId
                            });
                            let hyper = start.connectsTo.$buildRaw({
                                id: targetNode.endId
                            });
                            referenceNode = hyper.connectsFrom.$buildRaw(sourceNode.encode());
                        } else {
                            let start = Post.$buildRaw(sourceNode.encode());
                            referenceNode = start.connectsTo.$buildRaw(targetNode.encode());
                        }
                        referenceNode.$save({}).$then(response => {
                            response.graph.nodes.forEach( n => this.graph.addNode(n));
                            response.graph.edges.forEach( r => this.graph.addRelation(r));
                            this.graph.commit();
                        });
                    }
                }
                // TODO: else { connect without dragging only by clicking }
                // TODO: create self loops?

                this.isDragging = false;

                this.d3ConnectorLine.classed({
                    "moving": false
                });
                this.dragStartNode.d3NodeContainer.classed({
                    "selected": false
                });
            }

            onDragMoveEnd(d) {
                d.fixed &= ~6; // copied from force.drag
                if (this.isDragging) {
                    // if we were dragging before, the node should be fixed
                    this.setFixed(d);
                } else {
                    // if the user just clicked, the position should be reset.
                    // unsetFixed(graph, force, d);
                    // this is disabled, because we have the pin to unfix
                }

                this.isDragging = false;

                d.d3NodeContainer.classed({
                    "moving": false
                });
            }
        }

        let d3Graph = new D3Graph(scope.graph, element[0], scope.onClick, scope.onDraw);
        scope.controlGraph = d3Graph;
        d3Graph.init();
    }

    return {
        restrict: "A",
        scope: {
            graph: "=",
            controlGraph: "=",
            onClick: "&",
            onDraw: "&"
        },
        link
    };

}
