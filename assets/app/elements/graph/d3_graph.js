angular.module("wust.elements").directive("d3Graph", d3Graph);

d3Graph.$inject = ["$window", "DiscourseNode", "Helpers", "$location", "$filter", "Post", "ModalEditService", "EditService", "TagRelationEditService", "$q", "Auth", "ConnectedComponents", "LiveService"];

function d3Graph($window, DiscourseNode, Helpers, $location, $filter, Post, ModalEditService, EditService, TagRelationEditService, $q, Auth, ConnectedComponents, LiveService) {
    return {
        restrict: "A",
        scope: false,
        link
    };

    function link(scope, element) {
        let vm = scope.vm;

        class D3Graph {
            //TODO: rename onClick -> onNodeClick
            //TODO: use closure and have constants without this, also directly use vm?
            constructor(graph, rootDomElement, onClick = _.noop, onDraw = _.noop) {
                this.graph = graph;
                this.rootDomElement = rootDomElement;
                this.onClick = onClick;
                this.onDraw = onDraw;

                // settings
                this.visibleConvergence = false;
                this.debugDraw = false;
                this.hyperRelationAlignForce = 1;
                this.nodeVerticalForceFactor = 0;
                this.constantEdgeLength = true;
                this.stopForceOnPan = true;
                this.stopForceAfterNodeDrag = true;
                this.connectorLineOvershoot = 0;
                this.connectorLineArrowScale = 7;
                this.connectorLineArrowOffset = 0;
                this.markerUrl = _.endsWith($location.absUrl(), "/graph") ? $location.absUrl() : $location.absUrl() + "graph";
                this.arrowToResponse = false;
                this.dragHyperRelations = false;

                // state
                this.drawOnTick = this.drawOnTick = this.visibleConvergence;
                this.hoveredNode = undefined;
                this.width = rootDomElement.offsetWidth;
                this.height = rootDomElement.offsetHeight;
                this.dragInitiated = false; // if dragStart was triggered with the correct mouse button
                this.commitCount = 0;
                this.displayed = $q.defer();
                this.gotAllInitialData = $q.defer();

                // state for drag+drop
                this.isDragging = false;
                this.dragStartNode = undefined;
                this.dragStartNodeX = undefined;
                this.dragStartNodeY = undefined;
                this.dragStartMouseX = undefined;
                this.dragStartMouseY = undefined;
                this.dragStartOffsetX = undefined;
                this.dragStartOffsetY = undefined;

                this.linkStrength = 3; // originally this.force.linkStrength

                this.force = d3.layout.force()
                    .size([this.width, this.height])
                    .nodes(graph.nodes)
                    .links(graph.relations)
                    .linkStrength(this.constantEdgeLength ? 0.0 : 3.0) // rigidity, 0, because we handle this ourselves in tick()
                    .friction(0.92)
                    .linkDistance(50) // weak geometric constraint. Pushes nodes to achieve this distance
                    .charge(d => d.degree > 0 ? -1500 : -50)
                    .chargeDistance(1000)
                    .gravity(0.001)
                    .theta(0.8)
                    .alpha(0.1);
                this.zoom = d3.behavior.zoom().scaleExtent([0.1, 3]); // min/max zoom level

                // old config
                // .linkStrength(3) // rigidity
                // .friction(0.9)
                // // .linkDistance(120) // weak geometric constraint. Pushes nodes to achieve this distance
                // .linkDistance(d => connectsHyperEdge(d) ? 120 : 200)
                // .charge(d => d.hyperEdge ? -1500 : -1500)
                // .gravity(0.1)



            }

            init() {
                this.initDom();
                this.registerInitUIEvents();

                _.defer( () => {
                    // if graph is immediately displayed
                    if(this.rootDomElement.offsetWidth > 0 && this.rootDomElement.offsetHeight > 0) {
                        d3Graph.displayGraph();
                    } else {
                        // wait for event
                        scope.$on("display_graph", d3Graph.displayGraph.bind(this));
                    }
                });

                // call tick on every simulation step
                this.force.on("tick", this.tick.bind(this));

                // react on graph changes
                let deregisterCommit = this.graph.onCommit(this.updateGraph.bind(this));
                scope.$on("$destroy", deregisterCommit);

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
                    .attr("viewBox", this.arrowToResponse ? "-10 -3 10 6" : "0 -3 10 6") // x y w h
                    .attr("refX", this.arrowToResponse ? -9 : 9)
                    .attr("markerWidth", 15)
                    .attr("markerHeight", 9)
                    .attr("orient", "auto")
                    .append("svg:path")
                    .attr("d", this.arrowToResponse ? "M 0,-3 L -10,-0.5 L -10,0.5 L0,3" : "M 0,-3 L 10,-0.5 L 10,0.5 L0,3" )
                    .attr("class", "svgrelation"); // for the stroke color

                // svg-marker for connector line arrow
                this.d3SvgDefs.append("svg:marker")
                    .attr("id", "graph_connector_arrow")
                    .attr("viewBox", "0 -0.5 1 1")
                    .attr("refX", this.connectorLineArrowOffset / this.connectorLineArrowScale)
                    .attr("markerWidth", this.connectorLineArrowScale)
                    .attr("markerHeight", this.connectorLineArrowScale)
                    .attr("orient", "auto")
                    .append("svg:path")
                    .attr("d", "M 1,-0.3 L 0,-0.05 L 0,0.05 L1,0.3")
                    .attr("class", "connectorlinearrow"); // for the stroke color

                // choose the correct transform style for many browsers
                this.transformCompat = Helpers.cssCompat("transform", "Transform", "transform");
                this.transformOriginCompat = Helpers.cssCompat("transform-origin", "TransformOrigin", "transform-origin");

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
                        .attr("r", 30)
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
                }).style("marker-start", "url(" + this.markerUrl + "#graph_connector_arrow)");

                // add use element as the last(!) element to the svg, this controls the foremost element
                //http://stackoverflow.com/a/6289809
                // this.svgUseElement = this.d3NodeContainer.append("use").attr("id", "svgUseElement").attr("xlink:href", "#svgUseElement");
            }

            updateGraph(changes) {
                // console.log("updateGraph("+(this.commitCount+1)+")");
                //TODO: better have boolean?
                this.commitCount++;
                // the first commit is only the rootNode
                if(vm.isLoading && this.commitCount <= 1) return;
                // the second commit brings the rest of the graph and triggers the convergence

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
                .style("background-color", n => {
                    if(n.isHyperRelation) return undefined;
                    let tags = Helpers.sortedNodeTags(n);
                    if(tags.length === 0) return undefined;
                    return Helpers.smallPostBackgroundColor(tags[0]);
                })
                .style("border-color", n => {
                    let tags = Helpers.sortedNodeTags(n);
                    return !n.isHyperRelation && tags.length > 0 ? Helpers.postBorderColor(tags[0]) : undefined;
                })
                .html(d => {
                    //TODO: do it with d3 data-joins, or directly with the angular-port
                    if(d.isHyperRelation) {
                        let elem = document.createElement("span");
                        let tags = Helpers.sortedNodeTags(d);
                        tags.forEach(t => {
                            let tagLabel = document.createElement("span");
                            tagLabel.className = "tag-label nodetag";

                            if( t.isContext ) {
                                tagLabel.style.backgroundColor = Helpers.contextLabelBackgroundColor(t);
                                tagLabel.style.border = "1px solid " + Helpers.contextLabelBorderColor(t);
                            } else { // classification
                                tagLabel.style.backgroundColor = Helpers.classificationLabelBackgroundColor(t);
                                tagLabel.style.border = "1px solid " + Helpers.classificationLabelBorderColor(t);
                                tagLabel.style.borderRadius = Helpers.classificationLabelBorderRadius();
                            }
                            let content = document.createElement("span");
                            content.className = "content";
                            content.appendChild(document.createTextNode(t.title));
                            tagLabel.appendChild(content);
                            elem.appendChild(tagLabel);
                        });
                        return elem.outerHTML;
                    } else {
                        let elem = document.createElement("span");
                        elem.appendChild(document.createTextNode(d.title));
                        let circleCont = document.createElement("span");
                        circleCont.className = "tag_circle_container pull-right";
                        elem.appendChild(circleCont);
                        d.tags.forEach(t => {
                            let circleTitle = document.createElement("span");
                            circleTitle.className = "tag_circle_title";
                            circleTitle.setAttribute("title", t.title);
                            circleCont.appendChild(circleTitle);
                            let circle = document.createElement("span");
                            circle.className = "tag_circle";

                            if( t.isContext ) {
                                circle.style.backgroundColor = Helpers.contextCircleBackgroundColor(t);
                                circle.style.border = "1px solid " + Helpers.contextCircleBorderColor(t);
                                circle.style.borderRadius = Helpers.contextCircleBorderRadius();
                            } else { // classification
                                circle.style.backgroundColor = Helpers.classificationCircleBackgroundColor(t);
                                circle.style.border = "1px solid " + Helpers.classificationCircleBorderColor(t);
                            }

                            circleTitle.appendChild(circle);
                        });
                        return elem.outerHTML;
                    }
                }
                );

                let self = this;
                // add relations
                this.d3RelationPathWithData.enter()
                    .append("path")
                    .attr("class", "svgrelation")
                    .each(function(relation) {
                        // we have to check if we are connected to a hyperrelation,
                        // and if yes, if we are the start- ore endRelation.
                        // (response)--[startRelation]-->(H)--[endRelation]-->(post)
                        let isStartRelation = relation.target.isHyperRelation && relation.target.startId === relation.source.id;
                        let isEndRelation = relation.source.isHyperRelation && relation.source.endId === relation.target.id;
                        if( isStartRelation && self.arrowToResponse )
                            d3.select(this).style("marker-start", "url(" + self.markerUrl + "#graph_arrow)");

                        if( isEndRelation && !self.arrowToResponse )
                            d3.select(this).style("marker-end", "url(" + self.markerUrl + "#graph_arrow)");
                    });


                // tool buttons
                this.d3NodeTools = this.d3NodeContainerWithData.append("div")
                    .attr("class", "nodetools");

                this.d3NodeReplyTool = this.d3NodeTools.append("div")
                    .attr("class", "nodetool replytool fa fa-plus")
                    .style("display", d => (Auth.isLoggedIn) ? "inline-block" : "none");

                this.d3NodeConnectTool = this.d3NodeTools.append("div")
                    .attr("class", "nodetool connecttool icon-flow-line")
                    .style("display", d => (Auth.isLoggedIn && d.isHyperRelation.implies(this.arrowToResponse)) ? "inline-block" : "none")
                    .append("div").attr("class", "event-offset-rotate-fix");

                this.d3NodeEditTool = this.d3NodeTools.append("div")
                    .attr("class", "nodetool edittool fa fa-pencil")
                    .style("display", d => (Auth.isLoggedIn && d.isHyperRelation) ? "inline-block" : "none");

                this.d3NodePinTool = this.d3NodeTools.append("div")
                    .attr("class", "nodetool pintool fa fa-thumb-tack")
                    .style("display", d => (d.isHyperRelation && !this.dragHyperRelations) ? "none" : "inline-block");


                // this.d3NodeDeleteTool = this.d3NodeTools.append("div")
                //     .attr("class", "nodetool deletetool fa fa-trash");

                /// remove nodes and relations
                this.d3NodeContainerWithData.exit().remove();
                this.d3RelationPathWithData.exit().remove(); //

                // console.log(graph.nodes.map(n => n.id.slice(0,3)),d3NodeContainer.node());
                // console.log(graph.relations,d3RelationPath.node());


                this.updateGraphRefs();
                this.recalculateNodeDimensions(this.graph.nodes);

                this.force.nodes(this.graph.nodes); // nodes and relations get replaced instead of just changed by scalajs
                this.force.links(this.graph.relations); // that's why we need to set the new references

                // reinitialize the simulation, because we changes nodes and relations
                // https://github.com/mbostock/d3/blob/78e0a4bb81a6565bf61e3ef1b898ef8377478766/src/layout/force.js#L274
                this.force.start();

                // don't converge, we are triggering this manually in the beginning
                //TODO why? <= 2
                if(this.commitCount <= 2) this.force.stop();

                this.registerUIEvents();

                this.gotAllInitialData.resolve();
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
            let element = angular.element($window);
            let handler = this.resizeGraph.bind(this);
            element.bind("resize", handler);
            scope.$on("$destroy", () => element.unbind("resize", handler));
        }

        registerUIEvents() {
            //TODO: register only on added d3Nodes
            this.d3Node
                // dragging will trigger onClick on a node,
                // so register your action in onDragMoveEnd
                .on("mouseenter", d => scope.$apply(() => {
                    this.setNodeOffset(d);
                    this.hoveredNode = d;
                }))
                .on("mouseleave", d => {
                    scope.$apply(() => this.hoveredNode = undefined);
                    this.elementInfo.d3NodeContainer(d).classed({
                        "selected": false
                    });
                });

            this.d3Node.call(this.dragMove);
            this.d3Node.on("dblclick", this.loadMoreNodes.bind(this));
            this.d3NodePinTool.on("click", this.stopPropagationAfter(this.toggleFixed.bind(this))).call(this.disableDrag);
            this.d3NodeConnectTool.call(this.dragConnect);
            this.d3NodeEditTool.on("click", this.stopPropagationAfter(this.editNode.bind(this))).call(this.disableDrag);
            this.d3NodeReplyTool.on("click", this.stopPropagationAfter(this.replyToNode.bind(this))).call(this.disableDrag);
            // this.d3NodeDeleteTool.on("click", this.stopPropagationAfter(this.removeNode.bind(this))).call(this.disableDrag);
        }


        loadMoreNodes(d) {
            ConnectedComponents.$find(d.id, {depth: 1}).$then(response => {
                response.nodes.forEach(n => this.graph.addNode(n));
                response.relations.forEach(r => this.graph.addRelation(r));
                this.graph.commit();

                LiveService.registerNodes(this.graph.nodes);
            });
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

        converge(onConvergeFinish = _.noop) {
            // console.log("converge");
            // let convergeIterations = 0;
            this.initConverge();

            this.force.resume();

            if (this.visibleConvergence) {
                //TODO: why two times afterConverge? called also in nonBlockingConverge
                let afterConvergeOnce = _.once(this.afterConverge.bind(this)); // we don't know how to unsubscribe
                this.force.on("end", () => {
                    afterConvergeOnce();
                    onConvergeFinish();
                });
            } else {
                requestAnimationFrame(this.nonBlockingConverge.bind(this, onConvergeFinish));
            }
        }

        nonBlockingConverge(onConvergeFinish = _.noop) {
            let startTime = Date.now();
            // keep a constant frame rate
            while (((startTime + 300) > Date.now()) && (this.force.alpha() > 0)) {
                this.force.tick();
                // convergeIterations++;
            }

            if (this.force.alpha() > 0) {
                requestAnimationFrame(this.nonBlockingConverge.bind(this, onConvergeFinish));
            } else {
                this.afterConverge();
                onConvergeFinish();
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
            } else {
                // make sure we have the correct rect sizes for every node.
                // we need this for our custom link length calculation in tick().

                // give dimensions to svg and html container
                // (important for node size calculation
                this.resizeContainers();
                this.recalculateNodeDimensions(this.graph.nodes);
            }
        }

        afterConverge() {
            this.resizeContainers();

            this.setFixed(this.graph.rootNode);

            this.drawOnTick = true;
            this.onDraw();

            if (this.visibleConvergence)
                this.focusMarkedNodes();
            else
                this.focusMarkedNodes(0);

            this.d3HtmlContainer.classed({ "converged": true });
            this.d3SvgContainer.classed({ "converged": true });
        }

        updateGraphRefs() {
            // console.log("updateGraphRefs");
            // write dom element ref and rect into graph node
            // for easy lookup
            let newElementInfo = {
                domNodeContainer: {},
                d3NodeContainer: {},
                domNode: {},
                d3Node: {},
                domNodeFrame: {},
                d3NodeFrame: {},
                domPath: {},
                d3Path: {}
            };

            if (this.elementInfo === undefined) {
                this.elementInfo = {
                    data: newElementInfo,
                    domNodeContainer: function(n) { return this.data.domNodeContainer[n.id]; },
                    d3NodeContainer: function(n) { return this.data.d3NodeContainer[n.id]; },
                    domNode: function(n) { return this.data.domNode[n.id]; },
                    d3Node: function(n) { return this.data.d3Node[n.id]; },
                    domNodeFrame: function(n) { return this.data.domNodeFrame[n.id]; },
                    d3NodeFrame: function(n) { return this.data.d3NodeFrame[n.id]; },
                    domPath: function(r) { return this.data.domPath[r.startId + r.endId]; },
                    d3Path: function(r) { return this.data.d3Path[r.startId + r.endId]; }
                };
            }

            this.graph.nodes.forEach((n, i) => {
                //TODO: somehow we need to keep the old domNodeContainer and domNodeFrame for already existing nodes
                //i am not sure whether the indices will be correct then,
                // do not ask why i fixed it like this. i have no idea.

                newElementInfo.domNodeContainer[n.id] = this.elementInfo.domNodeContainer(n) || this.d3NodeContainerWithData[0][i];
                newElementInfo.d3NodeContainer[n.id] = d3.select(newElementInfo.domNodeContainer[n.id]);

                newElementInfo.domNode[n.id] = this.d3Node[0][i];
                newElementInfo.d3Node[n.id] = d3.select(newElementInfo.domNode[n.id]);

                newElementInfo.domNodeFrame[n.id] = this.elementInfo.domNodeFrame(n) || this.d3NodeFrame[0][i];
                newElementInfo.d3NodeFrame[n.id] = d3.select(newElementInfo.domNodeFrame[n.id]);
            });

            this.graph.relations.forEach((r, i) => {
                let id = r.startId + r.endId;
                newElementInfo.domPath[id] = this.d3RelationPathWithData[0][i];
                newElementInfo.d3Path[id] = d3.select(newElementInfo.domPath[id]);
            });

            this.elementInfo.data = newElementInfo;
        }

        recalculateNodeDimensions(nodes) {
            nodes.forEach(n => {
                let domNode = this.elementInfo.domNode(n);
                if(domNode) {
                    n.size = geometry.Vec2(
                        //TODO: remove default values, and correctly get sizes by quickly showing html elements
                        domNode.offsetWidth,
                        domNode.offsetHeight);
                }

            });
        }

        straightenHyperRelations(e) {
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
        }

        applyVerticalNodeForce(e) {
            // pull nodes with more more children up
            this.graph.nonHyperRelationNodes.forEach(node => {
                if (node.fixed !== true) {
                    node.y += (node.verticalForce - this.graph.nonHyperRelationNodes.length / 2) * e.alpha * this.nodeVerticalForceFactor;
                }
            });
        }

        cutLineLength(relation) {
            let line = this.relationLine(relation);
            let source = relation.source;
            let target = relation.target;
            let sourceRect = this.nodeRect(source);
            let targetRect = this.nodeRect(target);

            if( sourceRect.width === 0 || sourceRect.height === 0 ||
                    targetRect.width === 0 || targetRect.height === 0 ) {
                // this can happen, when the graph is changed, before it was displayed once.
                // e.g. when answering in neighbours-view
                return [line, line.length];
            }

            let cut = this.cutByRects(line, sourceRect, targetRect);
            if(cut) {
                return [cut, cut.length];
            } else {
                // when cut is not defined, the nodes are overlapping.
                // clamped is the line that is covered by the intersection of the nodes
                // the length is negative to push the nodes away from each other
                let clamped = line.clampBy(sourceRect).clampBy(targetRect);
                return [geometry.Line(clamped.end, clamped.start), -clamped.length];
            }
        }

        pushConstantEdgeLength(e) {
            // maintain a constant edge length
            // this.force.linkStrength is set to zero, to disable default d3 calculations.
            // we do that ourselves, because we have rectangles instead of circles.
            // if the distance only depends on the center of the rectangles,
            // the distances are not correct after clamping.
            this.graph.relations.forEach (relation => {
                // cut every relation line to the intersections with its incident node rectangles

                // gauss-seidel relaxation for links
                // https://github.com/mbostock/d3/blob/78e0a4bb81a6565bf61e3ef1b898ef8377478766/src/layout/force.js#L77

                let source = relation.source;
                let target = relation.target;
                let shouldDistance = this.force.linkDistance();
                let [currentLine, currentDistance] = this.cutLineLength(relation); // can be negative

                if( currentDistance > 0 )
                    relation.line = currentLine;
                else
                    relation.line = undefined;

                let vector = currentLine.vector;

                // x/y distance
                let x = vector.x;
                let y = vector.y;

                let alpha = this.force.alpha();

                let currentForce = alpha * this.linkStrength * (currentDistance - shouldDistance) / currentDistance;
                x *= currentForce;
                y *= currentForce;
                let forceWeight = source.weight / (target.weight + source.weight);

                if(!source.fixed) {
                    source.x += x * (1 - forceWeight);
                    source.y += y * (1 - forceWeight);
                }
                if(!target.fixed) {
                    target.x -= x * forceWeight;
                    target.y -= y * forceWeight;
                }
            });
        }

        // tick function, called in each step in the force calculation,
        // applies all kinds of additional forces
        tick(e) {
            this.straightenHyperRelations(e);
            this.applyVerticalNodeForce(e);
            if(this.constantEdgeLength)
                this.pushConstantEdgeLength(e);
            else {
                this.graph.relations.forEach( relation => relation.line = this.cutLineLength(relation)[0] );
            }


            if (this.drawOnTick)
                this.drawGraph();
        }

        drawNodes() {
            this.graph.nodes.forEach(node => {
                if(node.isHyperRelation) {
                    let rect = this.nodeRect(node);
                    let corner = rect.minCorner;
                    let angle = rect.angle;

                    this.elementInfo.domNodeContainer(node).style[this.transformOriginCompat] = `${corner.x}px ${corner.y}px`;
                    this.elementInfo.domNodeContainer(node).style[this.transformCompat] = `rotate(${angle}rad) translate(${corner.x}px, ${corner.y}px)`;
                } else {
                    let corner = this.nodeRect(node).minCorner;
                    this.elementInfo.domNodeContainer(node).style[this.transformCompat] = `translate(${corner.x}px, ${corner.y}px)`;
                }
            });
        }

        relationLine(relation) {
            let s = relation.source;
            let t = relation.target;
            return geometry.Line(
                    geometry.Vec2(s.x, s.y),
                    geometry.Vec2(t.x, t.y));
        }

        nodeRect(node) {
            if( node.isHyperRelation ) {
                let angle = geometry.Line(
                        geometry.Vec2(node.source.x, node.source.y),
                        geometry.Vec2(node.target.x, node.target.y)
                        ).vector.angle;

                // never have text written upside-down
                if(angle > Math.PI / 2 && angle < Math.PI) angle += Math.PI;
                if(angle < -Math.PI / 2 && angle > -Math.PI) angle += Math.PI;

                return geometry.RotatedRect(
                        geometry.Vec2(node.x, node.y),
                        node.size,
                        angle
                        );
            } else {
                return geometry.AARect(
                        geometry.Vec2(node.x, node.y),
                        node.size
                        );
            }
        }

        cutByRects(line, rect1, rect2) {
            let cut = line.cutBy(rect1);
            cut = cut ? cut.cutBy(rect2) : undefined;
            return cut;
        }

        drawRelations() {
            this.graph.relations.forEach(relation => {
                let line = relation.line;

                if( line === undefined ) {
                    let domPath = this.elementInfo.domPath(relation);
                    domPath.setAttribute("d", "");
                    return;
                }

                if (isNaN(line.x1) || isNaN(line.y1) || isNaN(line.x2) || isNaN(line.y2))
                    console.warn("invalid coordinates for relation");
                else {
                    let pathAttr = `M ${line.x1} ${line.y1} L ${line.x2} ${line.y2}`;
                    let domPath = this.elementInfo.domPath(relation);
                    domPath.setAttribute("d", pathAttr);
                }
            });
        }

        drawGraph() {
            if( this.graph.nodes[0].size.x === 0 )
                this.recalculateNodeDimensions(this.graph.nodes);

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

        resizeContainers() {
            this.width = this.rootDomElement.offsetWidth;
            this.height = this.rootDomElement.offsetHeight;
            this.d3Svg.style("width", this.width + "px").style("height", this.height + "px");
            this.d3Html.style("width", this.width + "px").style("height", this.height + "px");
        }

        // resize graph according to the current element dimensions
        resizeGraph(event) {
            // console.log("resizeGraph");
            let oldWidth = this.width;
            let oldHeight = this.height;

            // only do this on a real resize. (not on tab changes etc)
            // this also triggers when resizing the neighbour view
            if(oldWidth === 0 && oldHeight === 0) return;

            this.resizeContainers();

            this.moveOldCenterToNewCenter(oldWidth, oldHeight);
        }

        moveOldCenterToNewCenter(oldWidth, oldHeight, duration = 500) {
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

        displayGraph() {
            // console.log("triggered: display_graph");

            if( this.displayed.promise.$$state.status ) return;
            this.displayed.resolve();

            _.defer(() => {
                if( this.rootDomElement.offsetWidth === 0 ||
                        this.rootDomElement.offsetHeight === 0 )
                    console.warn("cannot display graph, root element has size 0");

                this.resizeContainers();

                this.gotAllInitialData.promise.then( () => {
                    // console.log("data is here, converge!");

                    // so focus the rootNode
                    this.converge(() => setTimeout(() => this.focusRootNode(), 700));
                });
            });
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
                let domNodeFrame = this.elementInfo.domNodeFrame(node);
                domNodeFrame.style.opacity = opacity;
                domNodeFrame.style.visibility = visibility;
            });

            // set relation visibility
            this.graph.relations.forEach((relation, i) => {
                let domPath = this.elementInfo.domPath(relation);
                domPath.style.visibility = relation.visible ? "inherit" : "hidden";
                domPath.style.opacity = (relation.source.marked === true && relation.target.marked === true) ? 1.0 : notMarkedOpacity;
            });
        }

        toggleFixed(d) {
            if (d.fixed) this.unsetFixed(d);
            else this.setFixed(d);
        }

        // fix the position of a given node
        setFixed(d) {
            d.fixed = true;
            this.elementInfo.d3NodeContainer(d).classed({
                "fixed": true
            });

            // the fixed class could change the elements dimensions
            this.recalculateNodeDimensions([d]);
            if(this.stopForceAfterNodeDrag) this.force.alpha(0);
        }

        // unfix the position of a given node
        unsetFixed(d) {
            d.fixed = false;
            this.elementInfo.d3NodeContainer(d).classed({
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
            }).$destroy().$then(response => {
                this.graph.removeNode(d.id);
                this.graph.commit();
                this.force.stop();
            }, response => humane.error(response.$response.data));
        }

        removeNode(d) {
            Post.$buildRaw({
                id: d.id
            }).$destroy().$then(response => {
                this.graph.removeNode(d.id);
                this.graph.commit();
                this.force.stop();
            }, response => humane.error(response.$response.data));
        }

        editNode(d) {
            if(d.isHyperRelation) {
                TagRelationEditService.show(d, () => this.disconnectHyperRelation(d));
            }
        }

        replyToNode(existingNode) {
            ModalEditService.show(existingNode);
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

            let domRect = this.elementInfo.domNode(d).getBoundingClientRect();
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

        onDragMoveInit(d, tolerance = 5, onStartDragging = () => {}, condition = d => true) {
            // check whether there was a substantial mouse movement. if
            // not, we will interpret this as a click event after the
            // mouse button is released (see onDragMoveEnd handler).
            let event = d3.event.sourceEvent;
            let diffX = this.dragStartMouseX - event.clientX;
            let diffY = this.dragStartMouseY - event.clientY;
            let diff = Math.sqrt(diffX * diffX + diffY * diffY);
            if (!this.isDragging && condition(d)) {
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
            this.onDragMoveInit(d, 5, () => this.elementInfo.d3NodeContainer(d).classed({
                "moving": true
            }), d => d.isHyperRelation.implies(this.dragHyperRelations));

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
                let dx = this.dragOffsetX + (event.clientX - this.dragStartMouseX) / scale;
                let dy = this.dragOffsetY + (event.clientY - this.dragStartMouseY) / scale;
                let a = Math.atan2(dy, dx);
                let endX = this.dragStartNodeX + dx + Math.cos(a) * this.connectorLineOvershoot;
                let endY = this.dragStartNodeY + dy + Math.sin(a) * this.connectorLineOvershoot;

                if (this.hoveredNode !== undefined && this.hoveredNode !== this.dragStartNode && (!this.arrowToResponse || !this.hoveredNode.isHyperRelation)) {
                    let line = geometry.Line(geometry.Vec2(endX, endY), geometry.Vec2(this.dragStartNodeX, this.dragStartNodeY));
                    let cut = this.cutByRects(line, this.nodeRect(this.hoveredNode), this.nodeRect(this.dragStartNode));
                    if( cut ) {
                        this.d3ConnectorLine
                            .attr("x1", cut.x1)
                            .attr("y1", cut.y1)
                            .attr("x2", cut.x2)
                            .attr("y2", cut.y2);
                    }
                    else {
                        this.d3ConnectorLine
                            .attr("x1", endX)
                            .attr("y1", endY)
                            .attr("x2", this.dragStartNodeX)
                            .attr("y2", this.dragStartNodeY);
                    }

                    this.elementInfo.d3NodeContainer(this.hoveredNode).classed({
                        "selected": true
                    });
                    this.elementInfo.d3NodeContainer(this.dragStartNode).classed({
                        "selected": true
                    });
                } else {
                    this.d3ConnectorLine
                        .attr("x1", endX)
                        .attr("y1", endY)
                        .attr("x2", this.dragStartNodeX)
                        .attr("y2", this.dragStartNodeY);

                    this.elementInfo.d3NodeContainer(this.dragStartNode).classed({
                        "selected": false
                    });
                }
            }
        }

        // we use dragend instead of click event, because it is emitted on mobile phones as well as on pcs
        onDragConnectEnd() {
            if (this.isDragging) {
                if (this.hoveredNode !== undefined ) {
                    let startNode = this.arrowToResponse ? this.hoveredNode : this.dragStartNode; // always normal node
                    let endNode = this.arrowToResponse ? this.dragStartNode : this.hoveredNode;

                    if(!startNode.isHyperRelation && startNode !== endNode) {
                        let existingConnects = _.find(this.graph.nodes, n => n.isHyperRelation && n.startId === startNode.id && n.endId === endNode.id);
                        if (existingConnects) {
                            // if there already is an existing connection, open the edit relation modal
                            this.editNode(existingConnects);
                        } else {
                            // TODO: we need to make it impossible to drag on incident relations, is assured by backend as long as verbose api is used
                            EditService.connectNodes(startNode, endNode).$then(response => {
                                let connects = _.find(response.graph.nodes, n => n.isHyperRelation && startNode.id === n.startId && endNode.id === n.endId);
                                if (connects === undefined) {
                                    console.warn(`cannot find connects relation for tag-modal: ${startNode} -> ${endNode}`);
                                    return;
                                }

                                connects.startNode = startNode;
                                connects.endNode = endNode;
                                TagRelationEditService.show(connects, () => this.disconnectHyperRelation(connects), true);
                            });
                        }
                    }
                }
            }
            // TODO: else { connect without dragging only by clicking }

            this.isDragging = false;
            // preview is reading isDragging, so angular needs to know that it changed
            scope.$apply();

            this.d3ConnectorLine.classed({
                "moving": false
            });
            this.elementInfo.d3NodeContainer(this.dragStartNode).classed({
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
                // if (!d.isHyperRelation)
                //     this.onClick(d);

                // if the user just clicked, the position should be reset.
                // unsetFixed(graph, force, d);
                // this is disabled, because we have the pin to unfix
            }

            this.isDragging = false;
            // preview is reading isDragging, so angular needs to know that it changed
            scope.$apply();
            this.setNodeOffset(d);

            this.elementInfo.d3NodeContainer(d).classed({
                "moving": false
            });
        }
    }

    let d3Graph = new D3Graph(vm.graph, element[0], vm.onClick, vm.onDraw);
    d3Graph.init();

    // expose some methods to the embedding directive
    vm.d3Info = {
        positionNode: (node, x, y) => {
            d3Graph.setNodePositionFromOffset(node, x, y);
            d3Graph.setFixed(node);
            d3Graph.drawGraph();
            if(node.degree > 0)
                d3Graph.force.resume();
        },
        filter: nodes => d3Graph.filter(nodes)
    };

    Object.defineProperties(vm.d3Info, {
        hoveredNode: {
            get: () => d3Graph.hoveredNode
        },
        isHovering: {
            get: () => d3Graph.hoveredNode && !d3Graph.hoveredNode.isHyperRelation && !d3Graph.isDragging
        }
    });
}
}
