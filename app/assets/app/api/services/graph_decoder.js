angular.module("wust.api").service("GraphDecoder", GraphDecoder);

GraphDecoder.$inject = ["$q"];

function GraphDecoder($q) {
    // expose restmod mixin
    this.mixin = {
        $hooks: {
            "after-fetch": constructGraphFromRecord
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


    function constructGraphFromRecord() {
        constructGraph(this);
    }

    function constructGraph(graph) {
        refreshIndex(graph);
        defineGraphMethods(graph);
    }

    function refreshIndex(graph) {
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
                predecessors: {
                    get: function() {
                        return _.map(this.inRelations, r => r.source);
                    }
                },
                successors: {
                    get: function() {
                        return _.map(this.outRelations, r => r.target);
                    }
                },
                neighbours: {
                    get: function() {
                        return this.predecessors.concat(this.successors);
                    }
                },
                inDegree: {
                    get: function() {
                        return this.inRelations.length;
                    }
                },
                outDegree: {
                    get: function() {
                        return this.outRelations.length;
                    }
                },
                degree: {
                    get: function() {
                        return this.inDegree + this.outDegree;
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
    }

    function defineGraphMethods(graph) {
        // also works on wrapped graphs!
        let nodeProperties = ["id", "title", "description", "label", "hyperEdge", "startId", "endId"];
        let relationProperties = ["startId", "endId", "label", "title"];

        let knownWrappers = [];
        let updateHandlers = [];

        graph.wrapped = function() {
            let wrapped = {
                "self": this
            };
            wrapped.nodes = _.map(this.nodes, n => new Decorator(n, nodeProperties));
            wrapped.edges = _.map(this.edges, r => new Decorator(r, relationProperties));
            wrapped.$pk = this.$pk;
            constructGraph(wrapped);
            knownWrappers.push(wrapped);

            return wrapped;
        };
        graph.subscribeUpdated = function(handler) {
            updateHandlers.push(handler);
        };
        graph.unsubscribeUpdated = function(handler) {
            _.remove(updateHandlers, handler);
        };
        graph.updated = function() {
            _.each(updateHandlers, handler => handler());
            _.each(knownWrappers, wrapper => wrapper.updated());
        };
        // TODO: having sets instead of arrays would be better...nodes,relations,inrelations,outrelations
        graph.addNode = function(node) {
            if (_.contains(this.nodes, node))
                return;

            this.nodes.push(node);
            _.each(knownWrappers, wrapper => wrapper.addNode(node));
        };
        graph.addRelation = function(relation) {
            if (_.contains(this.relations, relation))
                return;

            this.addNode(relation.source);
            if (!_.contains(relation.source.outRelations, relation))
                relation.source.outRelations.push(relation);

            this.addNode(relation.target);
            if (!_.contains(relation.target.inRelations, relation))
                relation.target.inRelations.push(relation);

            this.relations.push(relation);
            _.each(knownWrappers, wrapper => wrapper.addRelation(relation));
        };
        graph.removeNode = function(node) {
            _.remove(this.nodes, node);
            _.remove(this.relations, r => r.source === node || r.target === node);
            _.each(knownWrappers, wrapper => wrapper.removeNode(node));
        };
        graph.removeRelation = function(relation) {
            _.remove(this.relations, relation);
            _.remove(relation.target.inRelations, relation);
            _.remove(relation.source.outRelations, relation);
            _.each(knownWrappers, wrapper => wrapper.removeRelation(relation));
        };
        graph.rootNode = _.find(graph.nodes, {
            id: graph.$pk
        });
    }

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
