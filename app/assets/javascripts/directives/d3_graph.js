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
                    .linkDistance(100)
                    .size([width, height]);

                d3.select("svg").remove();

                // define events
                var zoom = d3.behavior.zoom().scaleExtent([0.1, 10]).on("zoom", zoomed);
                var drag = force.drag()
                    .on("dragstart", dragstarted)
                    .on("drag", dragged);

                var mainSvg = d3.select(element[0])
                    .append("svg")
                    .attr("width", width)
                    .attr("height", height)
                    .append("g")
                    .call(zoom)
                    .on("dblclick.zoom", null);

                var rectSvg = mainSvg.append("rect")
                    .attr("width", width)
                    .attr("height", height)
                    .style("fill", "none")
                    .style("pointer-events", "all");

                var svg = mainSvg.append("g");

                force
                    .nodes(graph.nodes)
                    .links(graph.edges)
                    .start();

                var link = svg.append("g").selectAll(".svglink")
                    .data(graph.edges)
                    .enter().append("line")
                    .attr("class", "svglink")
                    .style("stroke-width", d => d.strength);

                link
                    .append("title")
                    .text(d => d.label);

                var linktext = svg.append("g").selectAll("g.linklabelholder")
                    .data(graph.edges)
                    .enter().append("g").attr("class", "linklabelholder")
                    .append("text")
                    .attr("class", "linklabel")
                    .attr("text-anchor", "middle")
                    .text(d => d.label);

                var node = svg.append("g").selectAll(".svgnode")
                    .data(graph.nodes)
                    .enter().append("rect")
                    .attr("class", d => DiscourseNode.get(d.label).css + " svgnode")
                    .attr("width", 30)
                    .attr("height", 30)
                    .attr("rx", 4)
                    .attr("ry", 4)
                    .call(drag)
                    .on("dblclick", doubleclicked);

                node
                    .append("title")
                    .text(d => d.title);

                var nodetext = svg.append("g").selectAll("g.nodelabelholder")
                    .data(graph.nodes)
                    .enter().append("g").attr("class", "nodelabelholder")
                    .append("text")
                    .attr("class", "nodelabel")
                    .attr("text-anchor", "middle")
                    .text(d => d.title)
                    .on("click", onClick);

                force.on("tick", tick);

                function tick() {
                    link
                        .attr("x1", d => d.source.x)
                        .attr("y1", d => d.source.y)
                        .attr("x2", d => d.target.x)
                        .attr("y2", d => d.target.y);

                    linktext
                        .attr("transform", d => "translate(" + (d.source.x + d.target.x) / 2 + "," + (d.source.y + d.target.y) / 2 + ")");

                    node
                        .attr("x", d => d.x - 15) // TODO
                        .attr("y", d => d.y - 15);

                    nodetext.attr("transform", d => "translate(" + d.x + "," + d.y + ")");
                }

                function zoomed() {
                    svg.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
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
