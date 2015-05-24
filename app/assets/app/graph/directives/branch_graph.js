angular.module("wust.graph").directive("branchGraph", branchGraph);

branchGraph.$inject = [];

function branchGraph() {
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
                .attr("id", "arrow")
                .attr("viewBox", "0 -3 10 6")
                .attr("refX", 10)
                .attr("markerWidth", 10)
                .attr("markerHeight", 6)
                .attr("orient", "auto")
                .append("svg:path")
                .attr("d", "M 0,-3 L 10,-0.5 L 10,0.5 L0,3")
                .attr("class", "svglink"); // for the stroke color

            // create nodes in the svg
            let node = svg.append("g").attr("id","group_hypernodes-then-nodes")
                .selectAll()
                .data(graph.nodes).enter()
                .append("circle")
                .attr("cx", d => {d.x = 10 + d.xShift * 10; return d.x;})
                .attr("cy", d => {d.y = 10 + d.line * 50; return d.y;})
                .attr("r", 10)
                // .style("fill", d => d.newBranch !== undefined ? "#FFBC33" : "#464646");
                .style("fill", d => d3.scale.category20().range()[d.branch % 20]);

            // create edges in the svg
            let link = svg.append("g").attr("id","group_links")
                .selectAll()
                .data(graph.edges).enter()
                .append("path")
                .style("marker-end", "url(" + window.location.href + "#arrow)")
                .each(function(link) {
                    // if link is startRelation of a Hypernode
                    if( link.target.hyperEdge && link.target.startId === link.source.id ) {
                        d3.select(this).attr("class", "svglink");
                    } else {
                        d3.select(this).attr("class", "svglink arrow");
                    }
                })
            .attr("d",(link) => {
                return link.source === link.target ?  // self loop
                    `
                    M ${graph.nodes[link.source].x} ${graph.nodes[link.source].y}
                    m -20, 0
                    c -80,-80   120,-80   40,0
                    `
                 :
                    `
                    M ${graph.nodes[link.source].x} ${graph.nodes[link.source].y}
                    L ${graph.nodes[link.target].x} ${graph.nodes[link.target].y}
                    `;
            });

            // get the dimensions of a html element
            function getElementDimensions(elem) {
                return [elem.offsetWidth, elem.offsetHeight];
            }

            function freeShift(branches) {
                let usedShifts = _(branches).reject(b => (b.newBranch === undefined) || b.xShift === undefined).map(b => b.xShift).uniq().value();
                let freeShift = 0;
                while( freeShift < 1000 ) {
                    if(!_.contains(usedShifts, freeShift))
                        break;
                    freeShift++;
                }
                return freeShift;
            }

            function positionNodePredecessors(branches, predecessorMap, showFirstOfAllBranches = false, line = 0, nextBranchId = 0) {
                if(branches.length === 0) return;

                let nextBranch = _.min(branches, b => b.newBranch);
                let current = showFirstOfAllBranches ?
                    (nextBranch === Infinity ? _.first(branches) : nextBranch)
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
                            p.xShift = freeShift(branches.concat(predecessors));
                        }
                    });
                }

                positionNodePredecessors(predecessors.concat(_.without(branches, current)), predecessorMap, showFirstOfAllBranches, line + 1, nextBranchId);
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
                positionNodePredecessors([rootNode], predecessorMap, true);
            }

        });
    }
}
