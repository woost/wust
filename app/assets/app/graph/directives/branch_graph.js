angular.module("wust.graph").directive("branchGraph", branchGraph);

branchGraph.$inject = [];

function branchGraph() {
    return {
        restrict: "A",
        scope: {
            graph: "="
        },
        link: link
    };

    function link(scope, element) {
        // watch for changes in the ngModel
        scope.$watch("graph", graph => {
            if (graph.nodes === undefined)
                return;

            // get dimensions
            let [width, height] = getElementDimensions(element[0]);

            preprocessGraph(graph);

            // remove any previous svg
            d3.select("svg").remove();

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

            function preprocessGraph(graph) {
                _.each(graph.nodes, node => node.x = _.random(width));
                _.each(graph.nodes, node => node.y = _.random(height));
            }

        });
    }
}
