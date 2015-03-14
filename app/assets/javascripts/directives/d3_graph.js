angular.module("wust").directive('d3Graph', function(DiscourseNode) {
    return {
        restrict: 'A',
        require: '^ngModel',
        scope: {
            ngModel: '=',
            onClick: '&',
        },
        link: function(scope, element) {
            var onClick = scope.onClick() || function(d, i) {};

            scope.$watchCollection("ngModel", function() {
                var graph = scope.ngModel;

                var width = "100%";
                var height = "100%";

                //TODO: center
                var force = d3.layout.force()
                    .charge(-1200)
                    .linkDistance(200)
                    .size([800, 600]);

                d3.select("svg").remove();

                var svg = d3.select(element[0]).append("svg")
                    .attr("width", width)
                    .attr("height", height)
                    .attr("pointer-events", "all")
                    .append('svg:g')
                    .call(d3.behavior.zoom().on("zoom", redraw))
                    .append('svg:g');

                svg
                    .append('svg:rect')
                    .attr('width', width)
                    .attr('height', height)
                    .attr('fill', 'white');

                force
                    .nodes(graph.nodes)
                    .links(graph.edges)
                    .start();

                var link = svg.selectAll(".link")
                    .data(graph.edges)
                    .enter().append("line")
                    .attr("class", "link")
                    .style("stroke-width", function(d) {
                        return d.strength;
                    });

                link
                    .append("title")
                    .text(function(d) {
                        return d.label;
                    });

                var linktext = svg.selectAll("g.linklabelholder")
                    .data(graph.edges)
                    .enter().append("g").attr("class", "linklabelholder")
                    .append("text")
                    .attr("class", "linklabel")
                    .attr("text-anchor", "middle")
                    .text(function(d) {
                        return d.label;
                    });

                var node = svg.selectAll(".node")
                    .data(graph.nodes)
                    .enter().append("circle")
                    .attr("class", "node")
                    .attr("r", 30)
                    .style("fill", function(d) {
                        return DiscourseNode.get(d.label).color;
                    })
                    .call(force.drag);

                node
                    .append("title")
                    .text(function(d) {
                        return d.title;
                    });

                var nodetext = svg.selectAll("g.nodelabelholder")
                    .data(graph.nodes)
                    .enter().append("g").attr("class", "nodelabelholder")
                    .append("text")
                    .attr("class", "nodelabel")
                    .attr("text-anchor", "middle")
                    .text(function(d) {
                        return d.title;
                    })
                    .on("click", onClick);

                force.on("tick", tick);

                function tick() {
                    link
                        .attr("x1", function(d) {
                            return d.source.x;
                        })
                        .attr("y1", function(d) {
                            return d.source.y;
                        })
                        .attr("x2", function(d) {
                            return d.target.x;
                        })
                        .attr("y2", function(d) {
                            return d.target.y;
                        });

                    linktext
                        .attr("transform", function(d) {
                            return "translate(" + (d.source.x + d.target.x) / 2 + "," + (d.source.y + d.target.y) / 2 + ")";
                        });

                    node
                        .attr("cx", function(d) {
                            return d.x;
                        })
                        .attr("cy", function(d) {
                            return d.y;
                        });

                    nodetext
                        .attr("transform", function(d) {
                            return "translate(" + d.x + "," + d.y + ")";
                        });
                }

                function redraw() {
                    if (redraw.scale === d3.event.scale)
                        return;

                    redraw.scale = d3.event.scale;
                    svg.attr("transform", "translate(" + d3.event.translate + ")" + " scale(" + d3.event.scale + ")");
                }
            });
        }
    };
});
