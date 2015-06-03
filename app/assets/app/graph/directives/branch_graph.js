angular.module("wust.graph").directive("branchGraph", branchGraph);

branchGraph.$inject = ["DiscourseNode"];

function branchGraph(DiscourseNode) {
    return {
        restrict: "A",
        scope: {
            graph: "=",
            rootId: "=",
            onDraw: "&"
        },
        link: link
    };

    function link(scope, element) {
        let onDraw = scope.onDraw || _.noop;

        // watch for changes in the ngModel
        scope.graph.$then(data => {

            let graph = data; //TODO: was angular.copy(data). Publish graph more elegantly
            let neighbourMap;
            let predecessorMap;
            let successorMap;
            preprocessGraph(graph); // assigns node positions in .line and .xShift

            let radius = 10;
            let border = 3;
            let verticalDistance = 40;
            let horizontalDistance = radius + 2*border + 2;
            let paddingLeft = border + radius;
            let paddingTop = border + radius;
            let paddingBottom = border + radius;
            let paddingRight = border + radius;
            function branchColor(branch) {return d3.scale.category10().range()[branch % 10];}

            let width = paddingLeft + _.max(graph.nodes, n => n.xShift).xShift*horizontalDistance + paddingRight;
            let height = paddingTop + _.max(graph.nodes, n => n.line).line*verticalDistance + paddingBottom;

            // construct svg
            let svg = d3.select(element[0])
                .append("svg")
                .attr("width", width)
                .attr("height", height)
                .on("dblclick.zoom", null);

            svg.append("svg:defs").append("svg:marker")
                .attr("id", "branch_arrow")
                .attr("viewBox", "0 -3 10 6")
                .attr("refX", 10)
                .attr("markerWidth", 10)
                .attr("markerHeight", 6)
                .attr("orient", "auto")
                .append("svg:path")
                .attr("d", "M 0,-3 L 10,-0.5 L 10,0.5 L0,3");


            let edgesWithSquashedHyperEdges = _(graph.edges).map((edge) => {
                // point startHyperEdges to the hypernode target
                // then remove endHyperEdges
                let target = graph.nodes[edge.target];
                if( target._hidden )
                    edge.target = graph.nodes.indexOf(successorMap[target.id][0]);
                return edge;
            }).reject((edge) => graph.nodes[edge.source]._hidden).value();

            let linksvg = svg.append("g").attr("id","group_links")
                .selectAll()
                .data(edgesWithSquashedHyperEdges).enter()
                .append("path");
                // .style("marker-end", "url(" + window.location.href + "#branch_arrow)")

            // create nodes in the svg
            let node = svg.append("g").attr("id","group_hypernodes-then-nodes")
                .selectAll()
                .data(_.reject(graph.nodes,"_hidden")).enter()
                .append("circle")
                .attr("cx", d => {d.x = paddingLeft + d.xShift * horizontalDistance; return d.x;})
                .attr("cy", d => {d.y = paddingTop + d.line * verticalDistance; return d.y;})
                .attr("r", radius)
                .attr("class", d => d.hyperEdge ? "relation_label" : "branch_node " + DiscourseNode.get(d.label).css)
                .style("stroke", d => branchColor(d.branch))
                .style("stroke-width", border)
                .style("stroke-dasharray", d => d.hyperEdge ? "4 3" : "");


            // create edges in the svg
            let link = linksvg
                .each(function(link) {
                    let thisLink = d3.select(this);
                    // if link is startRelation of a Hypernode
                    if( link.target.hyperEdge && link.target.startId === link.source.id ) {
                        thisLink.attr("class", "svglink");
                    } else {
                        thisLink.attr("class", "svglink branch_arrow");
                    }

                    thisLink.style("stroke-width", border);
                    thisLink.style("stroke", branchColor(graph.nodes[link.source].branch));
                    // thisLink.style("stroke-dasharray", d => graph.nodes[link.source].newBranch !== undefined ? ""+(3+graph.nodes[link.source].newBranch/3)+" 5 5 5" : "");
                })
            .attr("d",(link) => {
                let a = graph.nodes[link.target]; // top
                let b = graph.nodes[link.source]; // bottom
                let r = 50;
                function sgn(x) {return x > 0 ? 1 : -1; }
                function abs(x) {return Math.abs(x); }
                return link.source === link.target ?  // if self loop
                    `
                    M ${a.x} ${a.y}
                    m -20, 0
                    c -80,-80   120,-80   40,0
                    `
                 : // else connect two nodes
                    // starts at lower node
                    // L ${s.x} ${t.y +50}
                    ( a.x === b.x ) ? // if nodes are on a vertical line
                        `
                        M ${a.x} ${a.y}
                        L ${b.x} ${b.y}
                        `
                    : // else draw a curve
                        // quadratic bezier
                        // `
                        // M ${a.x} ${a.y}
                        // L ${abs(a.x-b.x) < r ? a.x : b.x - r*sgn(b.x-a.x)} ${a.y}
                        // Q ${b.x} ${a.y}  ${b.x} ${a.y+r}
                        // L ${b.x} ${b.y}
                        // `
                        // cubic bezier
                        `
                        M ${a.x} ${a.y}
                        L ${abs(a.x-b.x) < r ? a.x : b.x - r*sgn(b.x-a.x)} ${a.y}
                        C ${b.x} ${a.y}  ${b.x} ${a.y}  ${b.x} ${a.y+r}
                        L ${b.x} ${b.y}
                        `
                    ;

            });

            // get the dimensions of a html element
            function getElementDimensions(elem) {
                return [elem.offsetWidth, elem.offsetHeight];
            }

            function findFreeShift(parentY, parentX, maxYShifts, maxWidth) {
                function free(x) {return maxYShifts[x] === undefined ? true : parentY >= maxYShifts[x];}
                let maxShift = maxYShifts.length - 1;
                let shiftL, shiftR;

                let x = parentX-1;
                while( x > 0 ) {
                    if(free(x)) {
                        shiftL = x;
                        break;
                    }
                    x--;
                }

                x = parentX;
                while( x < maxWidth ) {
                    if(free(x)) {
                        shiftR = x;
                        break;
                    }
                    x++;
                }
                if(shiftL !== undefined && shiftR !== undefined) {
                    if( shiftR <= maxShift )
                        return (parentX - shiftL) > (shiftR - parentX) ? shiftR : shiftL;
                    else return shiftL;
                } else {
                    if(shiftR !== undefined) return shiftR;
                    else if(shiftL !== undefined) return shiftL;
                    else return maxWidth;
                }
            }

            function positionNodePredecessors(branches, predecessorMap, maxWidth = 6, maxYShifts = [], nextLine = 0, nextBranchId = 0) {
                if(branches.length === 0) return;

                let current = branches[0];
                if(current.positioned) return; // found a circle
                current.positioned = true;

                let predecessors = predecessorMap[current.id] || [];

                // decide, which branch to take first
                // predecessors = _.sortBy(predecessors, p => p.title);

                if(current._hidden) {
                    // skip hypernodes which look like relations (degree == 2)
                    let p = predecessors[0];
                    p.branch = current.branch;
                    p.line = current.line;
                    p.xShift = current.xShift;
                } else {
                    current.branch = current.branch !== undefined ? current.branch : nextBranchId++; // can be 0
                    current.line = current.line !== undefined ? current.line : nextLine++; // can be 0
                    current.xShift = current.xShift || 0;

                    let isLine = predecessors.length === 1;
                    if(predecessors.length > 0) { // more than one child
                        _.each(predecessors, (p,i) => {
                            p.branch = isLine ? current.branch : nextBranchId++; // TODO: not in loop
                            p.line = nextLine++;
                            p.xShift = findFreeShift(current.line, current.xShift, maxYShifts, maxWidth);
                            maxYShifts[p.xShift] = p.line;
                        });
                    }
                }

                let nextBranches = predecessors.concat(_.tail(branches));
                positionNodePredecessors(nextBranches, predecessorMap, maxWidth, maxYShifts, nextLine, nextBranchId);
            }

            function preprocessGraph(graph) {
                predecessorMap = _(graph.edges).map(edge => {
                    let source = graph.nodes[edge.source];
                    let target = graph.nodes[edge.target];
                    return {
                        [target.id]: [source]
                    };
                }).reduce(_.partialRight(_.merge, (a, b) => {
                    return a ? a.concat(b) : b;
                }, _)) || {};

                successorMap = _(graph.edges).map(edge => {
                    let source = graph.nodes[edge.source];
                    let target = graph.nodes[edge.target];
                    return {
                        [source.id]: [target]
                    };
                }).reduce(_.partialRight(_.merge, (a, b) => {
                    return a ? a.concat(b) : b;
                }, _)) || {};

                neighbourMap = _(graph.edges).map(edge => {
                    let source = graph.nodes[edge.source];
                    let target = graph.nodes[edge.target];
                    return {
                        [target.id]: [source],
                        [source.id]: [target]
                    };
                }).reduce(_.partialRight(_.merge, (a, b) => {
                    return a ? a.concat(b) : b;
                }, _)) || {};

                //  || neighbourMap[node.id].length > 2
                _.each(graph.nodes, (node) => node._hidden = node.hyperEdge);

                let rootNode = _.find(graph.nodes, { id: scope.rootId });
                positionNodePredecessors([rootNode], predecessorMap, 100);

                onDraw();
            }

        });
    }
}
