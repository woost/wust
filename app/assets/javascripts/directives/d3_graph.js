angular.module("wust").directive("d3Graph", function(DiscourseNode) {
    return {
        restrict: "A",
        require: "^ngModel",
        scope: {
            ngModel: "=",
            onClick: "&",
        },
        link: function(scope, element) {
            var onClick = scope.onClick() || _.noop;

            scope.$watchCollection("ngModel", () => {
                var graph = scope.ngModel;

                var width = element[0].offsetWidth;
                var height = element[0].offsetHeight;

                var force = d3.layout.force()
                    .friction(0.90)
                    // .gravity(0.05)
                    .charge(-1000)
                    .linkDistance(120)
                    .size([width, height]);

                d3.select("svg").remove();

                // define events
                var zoom = d3.behavior.zoom().scaleExtent([0.1, 10]).on("zoom", zoomed);
                var drag = force.drag()
                    .on("dragstart", dragstarted)
                    .on("drag", dragged);

                var svg = d3.select(element[0])
                    .append("svg")
                    .attr("width", width)
                    .attr("height", height)
                    // .append("g")
                    .call(zoom)
                    .on("dblclick.zoom", null);

                svg.append("svg:defs").append("svg:marker")
                    .attr("id", "arrow")
                    .attr("viewBox", "0 -5 10 10")
                    .attr("refX", 6)
                    .attr("markerWidth", 3)
                    .attr("markerHeight", 3)
                    .attr("orient", "auto")
                    .attr("stroke", "green")
                    .attr("stroke-width", "2")
                    .append("svg:path")
                    .attr("d", "M0,-5L10,0L0,5")
                    .attr("fill", "red");


                var container = svg.append("g")
                    .style("pointer-events", "all");


                force
                    .nodes(graph.nodes)
                    .links(graph.edges)
                    .start();


                var link = container.selectAll(".svglink")
                    .data(graph.edges)
                    .enter().append("line")
                    .attr("class", "svglink")
                    .style("stroke-width", 1)
                    .style("stroke", "#999")
                    .style("marker-end", "url(#arrow)");

                var linktextSvg = container.append("g").selectAll("g.linklabelholder")
                    .data(graph.edges)
                    .enter().append("g");

                var linktextHtml = linktextSvg
                    .append("foreignObject")
                    .attr("width", 600)
                    .attr("height", 600)
                    .style("text-align", "center")
                    .append("xhtml:span")
                    .style("line-height", "600px")
                    // .style("text-shadow", "white -1px 0px, white 0px 1px, white 1px 0px, white 0px -1px")
                    .style("background", "white")
                    .style("padding", "1px")
                    .html(d => d.label);



                var node = container.append("g").selectAll(".svgnode")
                    .data(graph.nodes)
                    .enter().append("g")
                    .call(drag)
                    .on("dblclick", doubleclicked);
                // .on("click", onClick);

                var nodeHtml = node.append("foreignObject")
                    .attr("width", 600)
                    .attr("height", 600)
                    .style("text-align", "center")
                    // .style("opacity", "0.5")
                    .append("xhtml:div")
                    .style("margin-top", "290px")
                    .style("max-width", "150px")
                    .style("cursor", "move")
                    .attr("class", d => "node " + DiscourseNode.get(d.label).css)
                    .html(d => d.title);

                force.on("tick", tick);

                function tick() {
                    link
                        .attr("x1", d => d.source.x)
                        .attr("y1", d => d.source.y)
                        .attr("x2", d => d.target.x)
                        .attr("y2", d => d.target.y);

                    linktextSvg
                        .attr("transform", d => "translate(" + ((d.source.x + d.target.x) / 2 - 300) + "," + ((d.source.y + d.target.y) / 2 - 300) + ")");


                    node.attr("transform", d => "translate(" + (d.x - 300) + "," + (d.y - 300) + ")");
                }

                function zoomed() {
                    container.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
                }

                function doubleclicked(d) {
                    d3.select(this).classed("fixed", d.fixed = false);
                    // need to explicitly resume the force, otherwise the graph
                    // is stuck until a node is dragged
                    force.resume();
                }

                function dragstarted(d) {
                    d3.event.sourceEvent.stopPropagation();
                    d3.select(this).classed("fixed", d.fixed = true);
                }

                function dragged(d) {
                    d3.select(this).attr("cx", d.x = d3.event.x).attr("cy", d.y = d3.event.y);
                }
            });
        }
    };
});
