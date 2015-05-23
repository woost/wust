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
                .attr("cx", d => d.x)
                .attr("cy", d => d.y)
                .attr("r", 10)
                // .style("fill", d => d.newBranch !== undefined ? "#FFBC33" : "#464646");
                .style("fill", d => d3.scale.category10().range()[d.branch]);

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
                let usedShifts = _.uniq(_.filter(_.map(branches,b => b.xShift),n => !isNaN(n)));
                let freeShift = 0;
                while( freeShift < 100 ) {
                    if(!_.contains(usedShifts, freeShift))
                        break;
                    freeShift++;
                }
                return freeShift;
            }

            function positionNodePredecessors(branches, predecessorMap, line = 0, nextBranchId = 0) {
                if(branches.length === 0) return;

                let nextBranch = _.min(branches, b => b.newBranch);
                let current = nextBranch === Infinity ? _.first(branches) : nextBranch;

                if(current.branch === undefined)
                    current.branch = nextBranchId++;
                current.xShift = current.xShift || 0;
                current.line = line;

                current.x = 50 + current.xShift * 30;
                current.y = (line + 1) * 50;

                let predecessors = predecessorMap[current.id] || [];

                // decide, which branch to take first
                // predecessors = _.sortBy(predecessors, p => ...));

                if(predecessors.length > 0) {
                    _.first(predecessors).branch = current.branch;
                    _.first(predecessors).xShift = current.xShift;
                }
                if(predecessors.length > 1) {
                    _.each(predecessors, p => p.newBranch = line); // to know which branch to take next
                    _.each(_.tail(predecessors), p => p.branch = nextBranchId++);
                    _.each(_.tail(predecessors), p => p.xShift = freeShift(branches.concat(predecessors)));
                }

                positionNodePredecessors(predecessors.concat(_.without(branches, current)), predecessorMap, line + 1, nextBranchId);
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
                positionNodePredecessors([rootNode], predecessorMap);
            }

        });
    }
}
