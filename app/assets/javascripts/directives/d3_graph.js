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

                var width = "100%";
                var height = "100%";

                //TODO: center
                var force = d3.layout.force()
                    .gravity(0.05)
                    .charge(-400)
                    .linkDistance(150)
                    .size([800, 600]);

                d3.select("svg").remove();

                // define events
                var zoom = d3.behavior.zoom().scaleExtent([1, 10]).on("zoom", zoomed);
                var drag = force.drag()
                    .on("dragstart", dragstarted)
                    .on("drag", dragged);

                var mainSvg = d3.select(element[0])
                    .append("svg")
                    .attr("width", width)
                    .attr("height", height)
                    .append("g")
                    .call(zoom);

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

                var link = svg.append("g").selectAll(".link")
                    .data(graph.edges)
                    .enter().append("line")
                    .attr("class", "link")
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

                var node = svg.append("g").selectAll(".node")
                    .data(graph.nodes)
                    .enter().append("circle")
                    .attr("class", "node")
                    .attr("r", 30)
                    .style("fill", d => DiscourseNode.get(d.label).color)
                    .call(drag);

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
                        .attr("cx", d => d.x)
                        .attr("cy", d => d.y);

                    nodetext.attr("transform", d => "translate(" + d.x + "," + d.y + ")");
                }

                function zoomed() {
                    svg.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
                }

                function dragstarted() {
                    d3.event.sourceEvent.stopPropagation();
                }

                function dragged(d) {
                    d3.select(this).attr("cx", d.x = d3.event.x).attr("cy", d.y = d3.event.y);
                }
            });
        }
    };
});
