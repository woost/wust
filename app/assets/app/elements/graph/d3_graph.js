angular.module("wust.elements").directive("d3Graph", d3Graph);

d3Graph.$inject = ["$window", "DiscourseNode", "Helpers", "$location", "$filter", "Post", "ModalEditService", "EditService", "TagRelationEditService"];

function d3Graph($window, DiscourseNode, Helpers, $location, $filter, Post, ModalEditService, EditService, TagRelationEditService) {
    return {
        restrict: "A",
        scope: false,
        link
    };

    function link(scope, element) {
        let vm = scope.vm;

        class D3Graph {
            //TODO: rename onClick -> onNodeClick
            constructor(graph, rootDomElement, onClick = _.noop, onDraw = _.noop) {
                this.graph = graph;
                this.rootDomElement = rootDomElement;
                this.onClick = onClick;
                this.onDraw = onDraw;

                // settings
                this.visibleConvergence = false;
                this.debugDraw = false;
                this.hyperRelationAlignForce = 0.5;
                this.nodeVerticalForceFactor = 1;
                this.stopForceOnPan = true;
                this.stopForceAfterNodeDrag = true;

                // state
                this.drawOnTick = this.drawOnTick = this.visibleConvergence;
                vm.state.hoveredNode = undefined;
                this.width = rootDomElement.offsetWidth;
                this.height = rootDomElement.offsetHeight;
                this.dragInitiated = false; // if dragStart was triggered with the correct mouse button
                this.commitCount = 0;

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
                    .links(graph.relations)
                    .linkStrength(0.9) // rigidity
                    .friction(0.92)
                    .linkDistance(100) // weak geometric constraint. Pushes nodes to achieve this distance
                    .charge(-1300)
                    .chargeDistance(1000)
                    .gravity(0.01)
                    .theta(0.8)
                    .alpha(0.1);
                this.zoom = d3.behavior.zoom().scaleExtent([0.1, 3]); // min/max zoom level
            }

            init() {
                this.initDom();
                this.registerInitUIEvents();

                // call tick on every simulation step
                this.force.on("tick", this.tick.bind(this));
                // react on graph changes
                this.graph.onCommit(this.updateGraph.bind(this));

                this.updateGraph({
                    newNodes: this.graph.nodes
                });
            }

            initDom() {
                // svg will stay in background and only render the relations
                this.d3Svg = d3.select(this.rootDomElement)
                    .append("svg")
                    .attr("width", this.width)
                    .attr("height", this.height);
                // .style("background", "rgba(220, 255, 240, 0.5)");

                // has the same size and position as the svg
                // renders nodes and relation labels
                this.d3Html = d3.select(this.rootDomElement)
                    .append("div").attr("id", "d3Html");
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
                    .attr("class", "svgrelation"); // for the stroke color

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

                // svg and html each have full-size
                // containers with enabled pointer events
                // translate-styles for zoom/pan will be applied here
                this.d3SvgContainer = this.d3Svg.append("g").attr("id", "svgContainer");
                this.d3HtmlContainer = this.d3Html.append("div").attr("id", "htmlContainer");

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
                this.d3RelationPath = this.d3SvgContainer.append("g").attr("id", "relationContainer");

                this.d3ConnectorLine = this.d3SvgContainer.append("line").classed({
                    "connectorline": true
                }).style("marker-start", "url(" + $location.absUrl() + "#graph_connector_arrow)");

                // add use element as the last(!) element to the svg, this controls the foremost element
                //http://stackoverflow.com/a/6289809
                // this.svgUseElement = this.d3NodeContainer.append("use").attr("id", "svgUseElement").attr("xlink:href", "#svgUseElement");
            }

            updateGraph(changes) {
                this.commitCount++;
                // the first commit is only the rootNode
                if(this.commitCount <= 1) return;
                // the second commit brings the rest of the graph and triggers the convergence
                if(this.commitCount === 2) this.converge();

                //TODO: this really is an unwanted side effect, we should not sort nodes here
                //add nodes to svg, first hypernodes then nodes, so the normal
                //nodes are drawn on top of the hypernodes in the svg
                this.graph.setNodes(_.sortBy(this.graph.nodes, n => !n.isHyperRelation));

                this.calculateNodeVerticalForce();
                this.setInitialNodePositions();

                // console.log("------ update graph");
                // console.log(graph.nonHyperRelationNodes.map((n) => n.title), graph.hyperRelations.map((r) => r.source.title + " --> " + r.target.title));
                // create data joins
                // http://bost.ocks.org/mike/join/
                this.d3NodeContainerWithData = this.d3NodeContainer
                    .selectAll("div")
                    .data(this.graph.nodes, (d) => d.id);

                this.d3RelationPathWithData = this.d3RelationPath
                    .selectAll("path")
                    .data(this.graph.relations, (d) => d.startId + " --> " + d.endId);

                // add nodes
                this.d3NodeFrame = this.d3NodeContainerWithData.enter()
                    .append("div").attr("class", d => "nodeframe" + (d.isHyperRelation ? " nodeframe-hyperrelation" : ""));

                this.d3Node = this.d3NodeContainerWithData.append("div")
                .attr("class", d => d.isHyperRelation ? "hyperrelation" : "small_post_directive")
                .style("background-color", n => (n.tags.length > 0 && !n.isHyperRelation) ? Helpers.hashToHslBackground(n.tags[0]) : undefined)
                .style("border-color", n => n.tags.length > 0 ? Helpers.hashToHslBorder(n.tags[0]) : undefined)
                .html(d => {
                    //TODO: do it with d3 data-joins, or directly with the angular-port
                    //TODO FIXME: XSS
                    if(d.isHyperRelation) {
                        return _.values(d.tags).map(t => {
                            return `<span class="label nodetag" style="background: ${Helpers.hashToHslFill(t)};">${t.title}</span><br>`;
                        }).join("");
                    } else {
                        return `${d.title}
                        <span class="tag_circle_container pull-right">` +
                            _.values(d.tags).map(t => {
                                return `<span class="tag_circle_title" title="${t.title}">
                                    <span class="tag_circle" style="background-color: ${Helpers.hashToHslFill(t)};"></span>
                                    </span>`;
                            }).join("")+
                        "</span>";
                    }
                }
                );

                // add relations
                this.d3RelationPathWithData.enter()
                    .append("path")
                    .attr("class", "svgrelation")
                    .each(function(relation) {
                        // if relation is startRelation of a Hypernode
                        if (!(relation.target.isHyperRelation && relation.target.startId === relation.source.id)) {
                            d3.select(this).style("marker-end", "url(" + $location.absUrl() + "#graph_arrow)");
                        }
                    });


                // tool buttons
                this.d3NodeTools = this.d3NodeContainerWithData.append("div")
                    .attr("class", "nodetools");

                this.d3NodePinTool = this.d3NodeTools.append("div")
                    .attr("class", "nodetool pintool fa fa-thumb-tack");

                this.d3NodeConnectTool = this.d3NodeTools.append("div")
                    .attr("class", "nodetool connecttool icon-flow-line")
                    .append("div").attr("class", "event-offset-rotate-fix");

                this.d3NodeDisconnectTool = this.d3NodeTools.append("div")
                    .attr("class", "nodetool disconnecttool fa fa-scissors");

                this.d3NodeReplyTool = this.d3NodeTools.append("div")
                    .attr("class", "nodetool replytool fa fa-reply");

                // this.d3NodeDeleteTool = this.d3NodeTools.append("div")
                //     .attr("class", "nodetool deletetool fa fa-trash");

                /// remove nodes and relations
                this.d3NodeContainerWithData.exit().remove();
                this.d3RelationPathWithData.exit().remove(); //

                // console.log(graph.nodes.map(n => n.id.slice(0,3)),d3NodeContainer.node());
                // console.log(graph.relations,d3RelationPath.node());


                this.updateGraphRefs();
                this.recalculateNodeDimensions(this.graph.nodes);

                this.registerUIEvents();
                this.force.nodes(this.graph.nodes); // nodes and relations get replaced instead of just changed by scalajs
                this.force.links(this.graph.relations); // that's why we need to set the new references
                this.force.start();

                this.registerUIEvents();
            }

            stopPropagationAfter(func) {
                return d => {
                    d3.event.stopImmediatePropagation();
                    func(d);
                };
            }

            // from: http://stackoverflow.com/a/17111220/793909
            dragStartWithButton(button, func) {
                return d => {
                    if (d3.event.sourceEvent.which === button) {
                        this.dragInitiated = true;
                        func(d);
                    }
                    d3.event.sourceEvent.stopPropagation();
                };
            }
            dragWithButton(button, func) {
                return d => {
                    if (d3.event.sourceEvent.which === button && this.dragInitiated) {
                        func(d);
                    }
                    d3.event.sourceEvent.stopPropagation();
                };
            }
            dragEndWithButton(button, func) {
                return d => {
                    if (d3.event.sourceEvent.which === button && this.dragInitiated) {
                        func(d);
                        this.dragInitiated = false;
                    }
                    d3.event.sourceEvent.stopPropagation();
                };
            }

            registerInitUIEvents() {
                // define events
                this.zoom.on("zoom", this.zoomed.bind(this));

                this.dragMove = d3.behavior.drag()
                    .on("dragstart", this.dragStartWithButton(1, this.onDragMoveStart.bind(this)))
                    .on("drag", this.dragWithButton(1, this.onDragMove.bind(this)))
                    .on("dragend", this.dragEndWithButton(1, this.onDragMoveEnd.bind(this)));

                this.dragConnect = d3.behavior.drag()
                    .on("dragstart", this.dragStartWithButton(1, this.onDragConnectStart.bind(this)))
                    .on("drag", this.dragWithButton(1, this.onDragConnectMove.bind(this)))
                    .on("dragend", this.dragEndWithButton(1, this.onDragConnectEnd.bind(this)));

            this.disableDrag = d3.behavior.drag()
                .on("dragstart", () => d3.event.sourceEvent.stopPropagation());

            this.d3Html.call(this.zoom)
                .on("dblclick.zoom", null)
                .on("mousedown", () => {
                    if(this.stopForceOnPan) this.force.stop();
                })
                .on("dblclick", () => {
                    this.force.resume();
                });

            this.d3Svg.on("dblclick.zoom", null);

            // register for resize event
            angular.element($window).bind("resize", this.resizeGraph.bind(this));
        }

        registerUIEvents() {
            //TODO: register only on added d3Nodes
            this.d3Node.select("div")
                // dragging will trigger onClick here,
                // so register it in onDragMoveEnd
                // .on("click", this.ignoreHyperRelation(node => {
                //     this.onClick(node);
                // }))
                .on("mouseover", d => scope.$apply(() => {
                    this.setNodeOffset(d);
                    vm.state.hoveredNode = d;
                }))
                .on("mouseout", d => {
                    scope.$apply(() => vm.state.hoveredNode = undefined);
                    d.d3NodeContainer.classed({
                        "selected": false
                    });
                });

            this.d3Node.call(this.dragMove);
            this.d3NodePinTool.on("click", this.stopPropagationAfter(this.toggleFixed.bind(this))).call(this.disableDrag);
            this.d3NodeConnectTool.call(this.dragConnect);
            this.d3NodeDisconnectTool.on("click", this.stopPropagationAfter(this.disconnectHyperRelation.bind(this))).call(this.disableDrag);
            this.d3NodeReplyTool.on("click", this.stopPropagationAfter(this.replyToNode.bind(this))).call(this.disableDrag);
            // this.d3NodeDeleteTool.on("click", this.stopPropagationAfter(this.removeNode.bind(this))).call(this.disableDrag);
        }


        // executes specified function only for normal nodes, i.e.,
        // ignores hyperrelations
        ignoreHyperRelation(func) {
            return d => {
                // do nothing for hyperrelations
                if (d.isHyperRelation)
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
            this.graph.nonHyperRelationNodes.forEach(node => {
                let deepReplies = node.deepSuccessors.length - node.deepPredecessors.length;
                node.verticalForce = deepReplies;
            });

            _.sortBy(this.graph.nonHyperRelationNodes, "verticalForce").forEach((n, i) => n.verticalForce = i);
        }

        converge() {
            // let convergeIterations = 0;
            this.initConverge();

            if (this.visibleConvergence) {
                //TODO: why two times afterConverge? called also in nonBlockingConverge
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
            // focusMarkedNodes needs visible/marked nodes and relations
            this.graph.nodes.forEach(n => {
                n.marked = true;
                n.visible = true;
            });
            this.graph.relations.forEach(e => {
                e.visible = true;
            });
            if (this.visibleConvergence) {
                this.recalculateNodeDimensions(this.graph.nodes);
                this.focusMarkedNodes(0);
                this.d3HtmlContainer.classed({
                    "converged": true
                });
                this.d3SvgContainer.classed({
                    "converged": true
                });
            }

        }

        afterConverge() {
            this.resizeGraph();

            this.setFixed(this.graph.rootNode);

            this.drawOnTick = true;
            this.onDraw();
            if (this.visibleConvergence)
                this.focusMarkedNodes();
            else {
                this.focusMarkedNodes(0);
                setTimeout(() => this.focusRootNode(700), 0);
            }


            this.d3HtmlContainer.classed({
                "converged": true
            });
            this.d3SvgContainer.classed({
                "converged": true
            });
        }

        updateGraphRefs() {
            // write dom element ref and rect into graph node
            // for easy lookup
            this.graph.nodes.forEach((n, i) => {
                n.domNodeContainer = this.d3NodeContainerWithData[0][i];
                n.d3NodeContainer = d3.select(n.domNodeContainer);

                n.domNode = this.d3Node[0][i];
                n.d3Node = d3.select(n.domNode);

                //TODO: i am not sure whether the indices will be correct then,
                //but the old nodes should be in the tail of the node list
                n.domNodeFrame = n.domNodeFrame || this.d3NodeFrame[0][i];
                n.d3NodeFrame = n.d3NodeFrame || d3.select(n.domNodeFrame);
            });

            this.graph.relations.forEach((r, i) => {
                r.domPath = this.d3RelationPathWithData[0][i];
                r.d3Path = d3.select(r.domPath);
            });
        }

        recalculateNodeDimensions(nodes) {
            nodes.forEach(n => {
                if(n.domNode)
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
                    node.y += (node.verticalForce - this.graph.nonHyperRelationNodes.length / 2) * e.alpha * this.nodeVerticalForceFactor;
                }
            });

            if (this.drawOnTick)
                this.drawGraph();
        }

        drawNodes() {
            this.graph.nodes.forEach((node) => {
                node.domNodeContainer.style[this.transformCompat] = `translate(${node.x - node.rect.width / 2}px, ${node.y - node.rect.height / 2}px)`;
            });
        }

        drawRelations() {
            this.graph.relations.forEach((relation) => {
                // clamp every relation line to the intersections with its incident node rectangles
                let line = Helpers.clampLineByRects(relation, relation.source.rect, relation.target.rect);
                if (isNaN(line.x1) || isNaN(line.y1) || isNaN(line.x2) || isNaN(line.y2))
                    console.warn("invalid coordinates for relation");
                else {
                    let pathAttr = `M ${line.x1} ${line.y1} L ${line.x2} ${line.y2}`;
                    relation.domPath.setAttribute("d", pathAttr);
                }
            });
        }

        drawGraph() {
            this.drawNodes();
            this.drawRelations();
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
        resizeGraph(event, duration = 500) {
            let oldWidth = this.width;
            let oldHeight = this.height;
            this.width = this.rootDomElement.offsetWidth;
            this.height = this.rootDomElement.offsetHeight;
            this.d3Svg.style("width", this.width + "px").style("height", this.height + "px");
            this.d3Html.style("width", this.width + "px").style("height", this.height + "px");

            // this is the first graph display,
            // so focus the rootNode
            if(oldWidth === 0 && oldHeight === 0) {
                this.focusMarkedNodes(0);
                setTimeout(() => this.focusRootNode(), 200);
            }

            // only do this on a real resize. (not on tab changes etc)
            if(oldWidth !== 0 && oldHeight !== 0) {
                // move old center to new center
                let widthDiff = this.width - oldWidth;
                let heightDiff = this.height - oldHeight;
                let oldTranslate = this.zoom.translate();
                let translate = [oldTranslate[0] + widthDiff/2, oldTranslate[1] + heightDiff/2];

                if (duration > 0) {
                    this.d3HtmlContainer.transition().duration(duration).call(this.zoom.translate(translate).event);
                    this.d3SvgContainer.transition().duration(duration).call(this.zoom.translate(translate).event);
                } else {
                    // skip animation if duration is zero
                    this.d3HtmlContainer.call(this.zoom.translate(translate).event);
                    this.d3SvgContainer.call(this.zoom.translate(translate).event);
                }
            }


            // if graph was hidden when initialized,
            // all foreign objects have size 0
            // this call recalculates the sizes
            this.recalculateNodeDimensions(this.graph.nodes);

            this.drawGraph();
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

        // focus the marked nodes and scale zoom accordingly
        focusRootNode(duration = 500) {
            if (this.width === 0 || this.height === 0) return;
            if (!this.graph.rootNode) return;

            let rootNode = this.graph.rootNode;

            let center = [rootNode.x, rootNode.y];
            let scale = Math.max(1, this.zoom.scale());

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

            this.graph.nodes.forEach(node => {
                node.marked = _(matchingNodes).contains(node);
                node.visible = node.marked || _(component).contains(node);

            });

            this.graph.nodes.forEach(node => {
                if (node.isHyperRelation) {
                    //TODO: mark chains of hyperrelations
                    node.marked = node.marked || node.source.marked && node.target.marked;
                }
            });

            this.graph.relations.forEach(relation => {
                relation.visible = _(component).contains(relation.source) && _(component).contains(relation.target);
            });

            this.setVisibility();
            this.focusMarkedNodes();
        }

        // reset visibility of nodes after filtering
        setVisibility() {
            let notMarkedOpacity = 0.3;
            // set node visibility
            this.graph.nodes.forEach(node => {
                let opacity = (node.marked) ? 1.0 : notMarkedOpacity;
                let visibility = node.visible ? "inherit" : "hidden";
                node.domNodeFrame.style.opacity = opacity;
                node.domNodeFrame.style.visibility = visibility;
            });

            // set relation visibility
            this.graph.relations.forEach((relation, i) => {
                relation.domPath.style.visibility = relation.visible ? "inherit" : "hidden";
                relation.domPath.style.opacity = (relation.source.marked === true && relation.target.marked === true) ? 1.0 : notMarkedOpacity;
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
            this.recalculateNodeDimensions([d]);
            if(this.stopForceAfterNodeDrag) this.force.alpha(0);
        }

        // unfix the position of a given node
        unsetFixed(d) {
            d.fixed = false;
            d.d3NodeContainer.classed({
                "fixed": false
            });

            // the fixed class could change the elements dimensions
            this.recalculateNodeDimensions([d]);
            this.force.resume();
        }

        disconnectHyperRelation(d) {
            Post.$buildRaw({
                id: d.startId
            }).connectsTo.$buildRaw({
                id: d.endId
            }).$destroy().$then(response => {}, response => humane.error("Server error:\n" + response));
            this.graph.removeNode(d.id);
            this.graph.commit();
            this.force.stop();
        }

        removeNode(d) {
            Post.$buildRaw({
                id: d.id
            }).$destroy().$then(response => {}, response => humane.error("Server error:\n" + response));
            this.graph.removeNode(d.id);
            this.graph.commit();
            this.force.stop();
        }

        replyToNode(existingNode) {
            ModalEditService.show();
            ModalEditService.currentNode.setReference(existingNode);
        }

        setNodePositionFromOffset(node, x, y) {
            let scale = this.zoom.scale();
            let translate = this.zoom.translate();
            node.x = (x - translate[0]) / scale;
            node.y = (y - translate[1]) / scale;
            node.px = node.x;
            node.py = node.y;
        }

        // used to decide where to place preview
        setNodeOffset(node) {
            let scale = this.zoom.scale();
            let translate = this.zoom.translate();
            node.xOffset = node.x * scale + translate[0];
            node.yOffset = node.y * scale + translate[1];
            node.onLeftHalf = node.xOffset < this.width / 2;
            node.onUpperHalf = node.yOffset < this.height / 2;
        }

        onDragStartInit(d) {
            // prevent d3 from interpreting this as panning
            d3.event.sourceEvent.stopPropagation();

            let event = d3.event.sourceEvent;
            let scale = this.zoom.scale();
            let target = event.target || event.srcElement;

            this.dragStartNodeX = d.x;
            this.dragStartNodeY = d.y;
            this.dragStartMouseX = event.clientX;
            this.dragStartMouseY = event.clientY;
            this.dragStartNode = d;

            let domRect = d.domNode.getBoundingClientRect();
            let eventRect = target.getBoundingClientRect();

            // vector from dragged element to node center, scaled, plus click offset
            this.dragOffsetX = (eventRect.left - domRect.left - domRect.width / 2) / scale + event.offsetX;
            this.dragOffsetY = (eventRect.top - domRect.top - domRect.height / 2) / scale + event.offsetY;
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

        onDragMoveInit(d, tolerance = 5, onStartDragging = () => {}) {
            // check whether there was a substantial mouse movement. if
            // not, we will interpret this as a click event after the
            // mouse button is released (see onDragMoveEnd handler).
            let event = d3.event.sourceEvent;
            let diffX = this.dragStartMouseX - event.clientX;
            let diffY = this.dragStartMouseY - event.clientY;
            let diff = Math.sqrt(diffX * diffX + diffY * diffY);
            if (!this.isDragging) {
                if (diff > tolerance) {
                    this.isDragging = true;
                    // preview is reading isDragging, so angular needs to know that it changed
                    scope.$apply();
                    onStartDragging();
                }
            }
        }

        onDragMove(d) {
            //TODO: fails when zooming/scrolling and dragging at the same time
            this.onDragMoveInit(d, 5, () => d.d3NodeContainer.classed({
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

            //TODO: instant dragging without tolerance!
            this.onDragMoveInit(d, 0);

            if (this.isDragging) {
                // default positioning is center of node.
                // but we let node stay under grabbed position.
                this.d3ConnectorLine
                    .attr("x1", this.dragStartNodeX + this.dragOffsetX + (event.clientX - this.dragStartMouseX) / scale)
                    .attr("y1", this.dragStartNodeY + this.dragOffsetY + (event.clientY - this.dragStartMouseY) / scale);

                if (vm.state.hoveredNode !== undefined) {
                    vm.state.hoveredNode.d3NodeContainer.classed({
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
                if (vm.state.hoveredNode !== undefined) {
                    let startNode = this.dragStartNode; // always normal node
                    let endNode = vm.state.hoveredNode;
                    // starting on hypernodes is also forbidden,
                    // but we don't need to handle this, because
                    // the connect button does not exist on hyperRelations.
                    //TODO: we need to make it impossible to drag on self loops and incident relations,
                    //is assured by backend.
                    EditService.connectNodes(startNode, endNode).$then(response => {
                        console.log(response);
                        let connects = _.find(response.graph.nodes, n => n.isHyperRelation && startNode.id === n.startId && endNode.id === n.endId);
                        if (connects === undefined) {
                            console.warn(`cannot find connects relation for tag-modal: ${startNode} -> ${endNode}`);
                            return;
                        }

                        TagRelationEditService.show(connects);
                    });
                }
            }
            // TODO: else { connect without dragging only by clicking }

            this.isDragging = false;
            // preview is reading isDragging, so angular needs to know that it changed
            scope.$apply();

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
                // onClick event on node is triggered here
                this.onClick(d);

                // if the user just clicked, the position should be reset.
                // unsetFixed(graph, force, d);
                // this is disabled, because we have the pin to unfix
            }

            this.isDragging = false;
            // preview is reading isDragging, so angular needs to know that it changed
            scope.$apply();
            this.setNodeOffset(d);

            d.d3NodeContainer.classed({
                "moving": false
            });
        }
    }

    vm.d3Graph = new D3Graph(vm.graph, element[0], vm.onClick, vm.onDraw);
    vm.d3Graph.init();
}
}
