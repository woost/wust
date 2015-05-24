angular.module("wust.graph").directive("branchGraph", branchGraph);

branchGraph.$inject = ["DiscourseNode"];

function branchGraph(DiscourseNode) {
    return {
        restrict: "A",
        scope: {
            graph: "=",
            rootId: "="
        },
        link: link
    };

    function link(scope, element) {
        // watch for changes in the ngModel
        scope.graph.$then(data => {
            let [width, height] = getElementDimensions(element[0]);

            let graph = angular.copy(data);
            preprocessGraph(graph);

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

            let radius = 10;
            let border = 3;
            function branchColor(branch) {return d3.scale.category10().range()[branch % 10];}

            let linksvg = svg.append("g").attr("id","group_links")
                .selectAll()
                .data(graph.edges).enter()
                .append("path");
                // .style("marker-end", "url(" + window.location.href + "#branch_arrow)")

            // create nodes in the svg
            let node = svg.append("g").attr("id","group_hypernodes-then-nodes")
                .selectAll()
                .data(graph.nodes).enter()
                .append("circle")
                .attr("cx", d => {d.x = border + (1 + d.xShift) * (radius + 2*border + 2); return d.x;})
                .attr("cy", d => {d.y = border + radius + d.line * 50; return d.y;})
                .attr("r", radius)
                .attr("class", d => "branch_node " + DiscourseNode.get(d.label).css)
                .style("stroke", d => branchColor(d.branch))
                .style("stroke-width", border);

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

            function freeShift(branches, maxWidth) {
                let usedShifts = _(branches).reject(b => (b.newBranch === undefined) || b.xShift === undefined).map(b => b.xShift).uniq().value();
                let freeShift = 0;
                while( freeShift < maxWidth ) {
                    if(!_.contains(usedShifts, freeShift))
                        break;
                    freeShift++;
                }
                return freeShift;
            }

            function positionNodePredecessors(branches, predecessorMap, showFirstOfAllBranches = false, maxWidth = 6, line = 0, nextBranchId = 0) {
                if(branches.length === 0) return;

                let minNewBranch = _.min(branches, b => b.newBranch);
                let current = showFirstOfAllBranches ?
                    (minNewBranch === Infinity ? _.first(branches) : minNewBranch)
                    : _.first(branches);

                if(current.branch === undefined)
                    current.branch = nextBranchId++;
                current.xShift = current.xShift || 0;
                current.line = line;

                let predecessors = predecessorMap[current.id] || [];

                // decide, which branch to take first
                // predecessors = _.sortBy(predecessors, p => ...));

                if(predecessors.length > 0) {
                    let first = _.first(predecessors);
                    first.branch = current.branch;
                    first.xShift = current.xShift;
                }
                if(predecessors.length > 1) {
                    _.each(predecessors, (p,i) => {
                        p.newBranch = line; // to know which branch to take next
                        if( i > 0) { // only for tail
                            p.branch = nextBranchId++;
                            p.xShift = freeShift(branches.concat(predecessors), maxWidth);
                        }
                    });
                }

                positionNodePredecessors(predecessors.concat(_.without(branches, current)), predecessorMap, showFirstOfAllBranches, maxWidth, line + 1, nextBranchId);
            }

            function preprocessGraph(graph) {
                let predecessorMap = _(graph.edges).map(edge => {
                    let source = graph.nodes[edge.source];
                    let target = graph.nodes[edge.target];
                    return {
                        [target.id]: [source]
                    };
                }).reduce(_.partialRight(_.merge, (a, b) => {
                    return a ? a.concat(b) : b;
                }, _)) || {};

                let rootNode = _.find(graph.nodes, { id: scope.rootId });
                positionNodePredecessors([rootNode], predecessorMap, true, 100);
            }

        });
    }
}
