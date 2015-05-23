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
                .style("fill", "red");

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

            function positionNodePredecessors(startX, startY, predecessorMap, visited, node) {
                if (_.contains(visited, node))
                    return;

                let predecessors = predecessorMap[node.id] || [];
                visited.push(node);
                node.x = startX;
                node.y = startY;
                if (predecessors.length > 1) {
                    _.each(predecessors, p => {
                        startX += 50;
                        startY += 50;
                        positionNodePredecessors(startX, startY, predecessorMap, visited, p);
                    });
                }
                else if (predecessors.length === 1) {
                    positionNodePredecessors(startX, startY + 50, predecessorMap, visited, predecessors[0]);
                }
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

                positionNodePredecessors(20, 20, predecessorMap, [], _.find(graph.nodes, {
                    id: scope.rootId
                }));
            }

        });
    }
}
