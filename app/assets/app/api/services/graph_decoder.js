angular.module("wust.api").service("GraphDecoder", GraphDecoder);

GraphDecoder.$inject = ["$q"];

function GraphDecoder($q) {
    // extends the restmod record
    let extendedRecord = {};

    // expose restmod mixin
    this.mixin = {
        $hooks: {
            "after-fetch": refreshHook
        },
        $extend: {
            Record: {
                $wrappedPromise: wrappedPromise
            }
        },
        nodes: {
            decode: decodeNodes
        },
        edges: {
            decode: decodeEdges
        }
    };


    // convenience method for having a promise on a wrapped graph
    function wrappedPromise() {
        let deferred = $q.defer();
        // note: this points to the restmod record
        this.$then(graph => deferred.resolve(graph.wrapped()));

        return deferred.promise;
    }

    function decodeNodes(apiNodes) {
        //TODO: shouldn't the api return properly formatted nodes?
        _(apiNodes).select(n => n.hyperEdge).each(n => n.title = n.label.toLowerCase()).value();

        // sorting the nodes by hyperEdge puts normal nodes after hypernodes.
        // This ensures that normal nodes are always drawn on top of hypernodes.
        return _.sortBy(apiNodes, n => n.hyperEdge);
    }

    function decodeEdges(apiEdges) {
        let edges = _(apiEdges).map(edge => {
            // this.nodes refers to the restmod model
            return _.merge(edge, {
                // TODO: should be sent by the api
                title: edge.label.toLowerCase(),
                label: edge.label
            });
        }).value();

        return edges;
    }


    function refreshHook() {
        refreshIndex(this);
    }

    function refreshIndex(graph) {
        // also works on wrapped graphs!

        let nodeProperties = ["id", "title", "description", "label", "hyperEdge", "startId", "endId"];
        let relationProperties = ["startId", "endId", "label", "title"];

        // clear all neighbour information
        _.each(graph.nodes, n => {
            n.inRelations = [];
            n.outRelations = [];
            Object.defineProperties(n, {
                relations: {
                    get: function() {
                        return this.inRelations.concat(this.outRelations);
                    }
                },
                inNeighbours: {
                    get: function() {
                        return _.map(this.inRelations, r => r.source);
                    }
                },
                outNeighbours: {
                    get: function() {
                        return _.map(this.outRelations, r => r.target);
                    }
                },
                neighbours: {
                    get: function() {
                        return this.inNeighbours.concat(this.outNeighbours);
                    }
                }
            });
            n.component = function() {
                let visited = new Set();
                findNeighbours(this);
                return Array.from(visited);

                function findNeighbours(node) {
                    if (visited.has(node))
                        return;

                    visited.add(node);
                    _.each(node.neighbours, findNeighbours);
                }
            };
        });

        Object.defineProperties(graph, {
            hyperRelations: {
                get: function() {
                    return _(this.nodes).filter((n) => n.hyperEdge === true).value();
                }
            }
        });

        graph.wrapped = function() {
            let wrapped = {
                "self": this
            };
            wrapped.nodes = _.map(this.nodes, n => new Decorator(n, nodeProperties));
            wrapped.edges = _.map(this.edges, r => new Decorator(r, relationProperties));
            refreshIndex(wrapped);
            return wrapped;
        };

        // hehe
        _.each(graph.nodes, n => {
            // the id is a string, thus it won't be iterable but you can still
            // lookup nodes via their id: nodes[0] = first node, nodes["adsaf-c32"] = node with id "adsaf-c32"
            graph.nodes[n.id] = n;
        });

        // reinitialize neighbours
        _.each(graph.edges.concat(graph.hyperRelations), e => {
            e.source = graph.nodes[e.startId];
            e.target = graph.nodes[e.endId];
            e.source.outRelations.push(e);
            e.target.inRelations.push(e);
        });

        function Decorator(self, decorateProperties) {
            let properties = _(decorateProperties).map(prop => {
                return {
                    [prop]: {
                        get: () => self[prop],
                        set: val => self[prop] = val
                    }
                };
            }).reduce(_.merge);

            Object.defineProperties(this, properties);
        }
    }
}
