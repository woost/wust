angular.module("wust.graph").directive("branchGraph", branchGraph);

branchGraph.$inject = ["DiscourseNode"];

function branchGraph(DiscourseNode) {
    return {
        restrict: "A",
        scope: {
            graph: "=",
            onDraw: "&"
        },
        link: link
    };

    function link(scope, element) {
        let onDraw = scope.onDraw || _.noop;

        let graph = scope.graph;

        // globals which are set in preprocessGraph
        let lowestLine = 0;
        preprocessGraph(graph); // assigns node positions in .line and .xShift

        let radius = 10;
        let border = 3;
        let verticalDistance = 40;
        let horizontalDistance = radius + 2 * border + 2;
        let paddingLeft = border + radius;
        let paddingTop = border + radius;
        let paddingBottom = border + radius;
        let paddingRight = border + radius;

        function branchColor(branch) {
            return d3.scale.category10().range()[branch % 10];
        }

        let width = paddingLeft + _.max(graph.nodes, n => n.xShift).xShift * horizontalDistance + paddingRight;
        let height = paddingTop + (_.max(graph.nodes, n => n.line).line - lowestLine) * verticalDistance + paddingBottom;

        // construct svg
        let svg = d3.select(element[0])
            .append("svg")
            .attr("width", width)
            .attr("height", height)
            .on("dblclick.zoom", null);

        let relationsWithSquashedHyperRelations = _(graph.relations).reject(
            // remove relations between hidden nodes
            (relation) => relation.target._hidden && relation.source._hidden
        ).map((relation) => {
            // point startHyperRelations to the hypernode target
            // then remove endHyperRelations
            let target = relation.target;
            if (target._hidden)
                relation.target = target.successors[0];
            return relation;
        }).reject((relation) => relation.source._hidden).value();

        let linksvg = svg.append("g").attr("id", "group_links")
            .selectAll()
            .data(relationsWithSquashedHyperRelations).enter()
            .append("path");
        // .style("marker-end", "url(" + window.location.href + "#branch_arrow)")

        let focusLineY = paddingTop - (lowestLine + 0.5) * verticalDistance;
        let focusLine = svg.append("line")
            .attr("x1", 0)
            .attr("y1", focusLineY)
            .attr("x2", width)
            .attr("y2", focusLineY)
            .attr("stroke", "#343434")
            .style("stroke-width", 3)
            .style("stroke-dasharray", "3 3");

        // create nodes in the svg
        let node = svg.append("g")
            .selectAll()
            .data(_.reject(graph.nodes, "_hidden")).enter()
            .append("circle")
            .attr("cx", d => {
                d.x = paddingLeft + d.xShift * horizontalDistance;
                return d.x;
            })
            .attr("cy", d => {
                d.y = paddingTop + (d.line - lowestLine) * verticalDistance;
                return d.y;
            })
            .attr("r", radius)
            .attr("class", d => d.isHyperRelation ? "hyperrelation" : "branch_node " + DiscourseNode.get(d.label).css)
            .style("stroke", n => n.leaf === true ? "black" : (n.directSuccessor === true ? "#666" : branchColor(n.branch)))
            .style("stroke-width", border)
            .style("stroke-dasharray", d => d.isHyperRelation ? "4 3" : "");

        // create relations in the svg
        let link = linksvg
            .each(function(link) {
                let thisLink = d3.select(this);

                // if link is startRelation of a Hypernode
                // if( target.isHyperRelation && target.startId === source.id ) {
                //     thisLink.attr("class", "svglink");
                // } else {
                thisLink.attr("class", "svglink branch_arrow");
                // }

                thisLink.style("stroke-width", border);
                thisLink.style("stroke", branchColor(link.source.branch));
                // thisLink.style("stroke-dasharray", d => link.source.newBranch !== undefined ? ""+(3+link.source.newBranch/3)+" 5 5 5" : "");
            })
            .attr("d", (link) => {
                let a = link.target; // top
                let b = link.source; // bottom
                let r = 50;

                function sgn(x) {
                    return x > 0 ? 1 : -1;
                }

                function abs(x) {
                    return Math.abs(x);
                }
                return link.source === link.target ? // if self loop
                    `
                    M ${a.x} ${a.y}
                    m -20, 0
                    c -80,-80   120,-80   40,0
                    ` : // else connect two nodes
                    // starts at lower node
                    // L ${s.x} ${t.y +50}
                    (a.x === b.x) ? // if nodes are on a vertical line
                    `
                        M ${a.x} ${a.y}
                        L ${b.x} ${b.y}
                        ` : // else draw a curve
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
                        `;

            });

        // get the dimensions of a html element
        function getElementDimensions(elem) {
            return [elem.offsetWidth, elem.offsetHeight];
        }

        function findFreeShift(parentY, parentX, maxYShifts, maxWidth) {
            function free(x) {
                return maxYShifts[x] === undefined ? true : parentY >= maxYShifts[x];
            }
            let maxShift = maxYShifts.length - 1;
            let shiftL, shiftR;

            let x = parentX - 1;
            while (x > 0) {
                if (free(x)) {
                    shiftL = x;
                    break;
                }
                x--;
            }

            x = parentX;
            while (x < maxWidth) {
                if (free(x)) {
                    shiftR = x;
                    break;
                }
                x++;
            }
            if (shiftL !== undefined && shiftR !== undefined) {
                if (shiftR <= maxShift)
                    return (parentX - shiftL) > (shiftR - parentX) ? shiftR : shiftL;
                else return shiftL;
            } else {
                if (shiftR !== undefined) return shiftR;
                else if (shiftL !== undefined) return shiftL;
                else return maxWidth;
            }
        }

        function positionNodePredecessors(branches, maxWidth = 6, maxYShifts = [], nextLine = 0, nextBranchId = 0) {
            if (branches.length === 0) return;

            let current = branches[0];
            if (current.positioned) return; // found a circle
            current.positioned = true;

            let predecessors = current.predecessors;

            // decide, which branch to take first
            // predecessors = _.sortBy(predecessors, p => p.title);

            if (current._hidden) {
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
                if (predecessors.length > 0) { // more than one child
                    _.each(predecessors, (p, i) => {
                        p.branch = isLine ? current.branch : nextBranchId++; // TODO: not in loop
                        p.line = nextLine++;
                        p.xShift = findFreeShift(current.line, current.xShift, maxYShifts, maxWidth);
                        maxYShifts[p.xShift] = p.line;
                    });
                }
            }

            let nextBranches = predecessors.concat(_.tail(branches));
            positionNodePredecessors(nextBranches, maxWidth, maxYShifts, nextLine, nextBranchId);
        }

        function preprocessGraph(graph) {
            function showPredecessors(node) {
                if (node._hidden === false) return;
                node._hidden = false;

                _.each(node.predecessors, showPredecessors);
            }

            function hideAllSuccessors(node) {
                showPredecessors(node);
                _.each(graph.nodes, (node) => {
                    node._hidden = node._hidden !== false;
                });
            }

            function showDirectAndLeafSuccessors(node) {
                let direct = [];
                let leaf = [];

                _.each(node.successors, (s) => { // hypernodes
                    _.each(s.successors, (s) => { // posts
                        s.directSuccessor = true;
                        direct.push(s);
                    });
                });

                function showLeafSuccessorsRec(node) {
                    //TODO: circle detection
                    if (node.outDegree === 0) { // node is leaf
                        node.leaf = true;
                        leaf.push(node);
                    } else {
                        _.each(node.successors, showLeafSuccessorsRec);
                    }
                }
                showLeafSuccessorsRec(node);

                let xShift = direct.length;
                _.each(direct, (s) => {
                    s.xShift = --xShift;
                    s._hidden = false;
                    s.line = --lowestLine;
                    s.branch = 0; //TODO: different colors per branch
                });

                leaf = _.difference(leaf, direct);
                xShift = leaf.length;
                _.each(leaf, (s) => {
                    s.xShift = --xShift;
                    s._hidden = false;
                    s.line = --lowestLine;
                    s.branch = 0; //TODO: different colors per branch
                });
            }

            function hideLonelyHyperNodes() {
                _.each(graph.nodes, (node) => node._hidden = node._hidden || (node.isHyperRelation && node.degree <= 2));
            }

            let rootNode = graph.rootNode;

            hideAllSuccessors(rootNode);
            showDirectAndLeafSuccessors(rootNode);
            hideLonelyHyperNodes();

            positionNodePredecessors([rootNode], 100);

            onDraw();
        }
    }
}
