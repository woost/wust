app.directive('d3Graph', function(DiscourseNode) {
    return {
        restrict: 'A',
        require: '^ngModel',
        scope: {
            ngModel: '=',
            onClick: '&',
            options: '='
        },
        link: function(scope, element) {
            // var network = new vis.Network(element[0], scope.ngModel, scope.options || {});
            // var onClick = scope.onClick() || function(prop) {};
            // network.on('click', function(properties) {
            //     onClick(properties);
            // });

            scope.$watchCollection("ngModel", function() {
                var graph = scope.ngModel;

                var color = d3.scale.category20();

                //TODO: center
                var force = d3.layout.force()
                    .charge(-120)
                    .linkDistance(30)
                    .size([800, 600]);

                d3.select("svg").remove();

                var svg = d3.select(element[0]).append("svg")
                    .attr("width", "100%")
                    .attr("height", "100%");

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

                link.append("title")
                    .text(function(d) {
                        return d.title;
                    });

                var node = svg.selectAll(".node")
                    .data(graph.nodes)
                    .enter().append("circle")
                    .attr("class", "node")
                    .attr("r", 5)
                    .style("fill", function(d) {
                        return DiscourseNode.get(d.label).color;
                    })
                    .call(force.drag);

                node.append("title")
                    .text(function(d) {
                        return d.title;
                    });

                force.on("tick", function() {
                    link.attr("x1", function(d) {
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

                    node.attr("cx", function(d) {
                        return d.x;
                    })
                        .attr("cy", function(d) {
                            return d.y;
                        });
                });
            });
        }
    };
});
