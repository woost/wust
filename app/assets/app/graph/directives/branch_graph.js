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

            // create edges in the svg
            let link = svg.append("g").attr("id","group_links")
                .selectAll()
                .data(graph.edges).enter()
                .append("path")
                .each(function(link) {
                    // if link is startRelation of a Hypernode
                    if( link.target.hyperEdge && link.target.startId === link.source.id ) {
                        d3.select(this).attr("class", "svglink");
                    } else {
                        d3.select(this).attr("class", "svglink arrow");
                    }
                });

            // create nodes in the svg
            let node = svg.append("g").attr("id","group_hypernodes-then-nodes")
                .selectAll()
                .data(graph.nodes).enter()
                .append("circle")
                .attr("cx", d => _.random(width))
                .attr("cy", d => _.random(height))
                .attr("r", 10)
                .style("fill", "red");

            // get the dimensions of a html element
            function getElementDimensions(elem) {
                return [elem.offsetWidth, elem.offsetHeight];
            }
        });
    }
}
