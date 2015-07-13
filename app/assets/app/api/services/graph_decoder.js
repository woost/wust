angular.module("wust.api").service("GraphDecoder", GraphDecoder);

GraphDecoder.$inject = ["$q", "UniqArr"];

function GraphDecoder($q, UniqArr) {
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


    // define common properties of nodes and relations
    let nodeProperties = ["id", "title", "description", "label", "hyperEdge", "startId", "endId"];
    let relationProperties = ["startId", "endId", "label", "title"];

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
        defineGraphRecordMethods(this);
    }

    function constructGraph(graph) {
        _.each(graph.nodes, defineNodeProperties);

        Object.defineProperties(graph, {
            hyperRelations: {
                get: function() {
                    return this.cached.hyperRelations(this);
                }
            },
            nonHyperRelationNodes: {
                get: function() {
                    return this.cached.nonHyperRelationNodes(this);
                }
            }
        });

        refreshIndex(graph);
        defineGraphMethods(graph);
    }

    function defineNodeProperties(n) {
        n.inRelations = UniqArr.relation();
        n.outRelations = UniqArr.relation();

        Object.defineProperties(n, {
            relations: {
                get: function() {
                    return this.inRelations.concat(this.outRelations);
                }
            },
            predecessors: {
                get: function() {
                    return this.cached.predecessors(this);
                }
            },
            successors: {
                get: function() {
                    return this.cached.successors(this);
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
            },
            component: {
                get: function() {
                    return this.cached.component(this);
                }
            },
            deepSuccessors: {
                get: function() {
                    return this.cached.deepSuccessors(this);
                }
            },
            deepPredecessors: {
                get: function() {
                    return this.cached.deepPredecessors(this);
                }
            }
        });
        // $encode method to get the original
        n.$encode = function() {
            return _.pick(this, nodeProperties);
        };
    }

    function depthFirstSearch(startNode, nextNodes) {
        let visited = new Set();
        visitNext(startNode);
        return Array.from(visited);

        function visitNext(node) {
            if (visited.has(node)) return;
            visited.add(node);

            _.each(nextNodes(node), visitNext);
        }
    }

    function calculateComponent(startNode) {
        return depthFirstSearch(startNode, (node) => node.neighbours);
    }

    function calculateDeepSuccessors(startNode) {
        return depthFirstSearch(startNode, (node) => node.successors);
    }

    function calculateDeepPredecessors(startNode) {
        return depthFirstSearch(startNode, (node) => node.predecessors);
    }

    function calculateSuccessors(node) {
        let successors = _.reject(_.map(node.outRelations, "target"), "hyperEdge");
        if (node.hyperEdge) {
            successors = _.reject(successors, {
                id: node.endId
            });
        }

        return successors;
    }

    function calculatePredecessors(node) {
        let predecessors = _.reject(_.map(node.inRelations, "source"), "hyperEdge");
        if (node.hyperEdge) {
            predecessors = _.reject(predecessors, {
                id: node.startId
            });
        }

        return predecessors;
    }

    function invalidateNodeCache(node) {
        node.cached = {
            component: _.once(calculateComponent),
            deepSuccessors: _.once(calculateDeepSuccessors),
            deepPredecessors: _.once(calculateDeepPredecessors),
            predecessors: _.once(calculatePredecessors),
            successors: _.once(calculateSuccessors)
        };
    }

    function calculateNonHyperRelationNodes(graph) {
        return _.reject(graph.nodes, "hyperEdge");
    }

    function calculateHyperRelations(graph) {
        return _.select(graph.nodes, "hyperEdge");
    }

    function invalidateGraphCache(graph) {
        _.each(graph.nodes, n => invalidateNodeCache(n));
        graph.cached = {
            hyperRelations: _.once(calculateHyperRelations),
            nonHyperRelationNodes: _.once(calculateNonHyperRelationNodes)
        };
    }

    function refreshIndex(graph) {
        let nodesById = {};
        graph.nodes.byId = id => nodesById[id];
        _.each(graph.nodes, n => {
            nodesById[n.id] = n;
        });

        // reinitialize neighbours
        // note: we need to use calculateHyperRelations here instead of the cached
        // properties as we need to wait until the in- and outrelations of all
        // nodes are calculated before setting the cache.
        _.each(graph.edges.concat(calculateHyperRelations(graph)), e => {
            //TODO: rename to starNode/endNode and provide wrapping function for d3, which implements source/target
            e.source = graph.nodes.byId(e.startId);
            e.target = graph.nodes.byId(e.endId);
            e.source.outRelations.push(e);
            e.target.inRelations.push(e);
        });

        invalidateGraphCache(graph);
    }

    function defineGraphRecordMethods(graph) {
        let knownWrappers = [];

        graph.wrapped = function() {
            let wrapped = {
                "self": this
            };
            wrapped.nodes = UniqArr.node(_.map(this.nodes, n => new Decorator(n, nodeProperties)));
            wrapped.edges = UniqArr.relation(_.map(this.edges, r => new Decorator(r, relationProperties)));
            wrapped.$pk = this.$pk;
            constructGraph(wrapped);

            wrapped.commit = function() {
                this.self.commit();
            };
            wrapped.addNode = function(node) {
                this.self.addNode(node);
            };
            wrapped.addRelation = function(relation) {
                this.self.addRelation(relation);
            };
            wrapped.removeNode = function(node) {
                this.self.removeNode(node);
            };
            wrapped.removeRelation = function(relation) {
                this.self.removeRelation(relation);
            };

            knownWrappers.push(wrapped);
            return wrapped;
        };
        //TODO: rename to commit!
        graph.commit = function() {
            this.commitInternal();
            _.each(knownWrappers, wrapper => wrapper.commitInternal());
        };
        graph.addNode = function(node) {
            this.addNodeInternal(node);
            _.each(knownWrappers, wrapper => wrapper.addNodeInternal(new Decorator(node, nodeProperties)));
        };
        graph.addRelation = function(relation) {
            this.addRelationInternal(relation);
            _.each(knownWrappers, wrapper => wrapper.addRelationInternal(new Decorator(relation, relationProperties)));
        };
        graph.removeNode = function(node) {
            this.removeNodeInternal(node);
            _.each(knownWrappers, wrapper => wrapper.removeNodeInternal(node));
        };
        graph.removeRelation = function(relation) {
            this.removeRelationInternal(relation);
            _.each(knownWrappers, wrapper => wrapper.removeRelationInternal(relation));
        };
    }

    function defineGraphMethods(graph) {
        let updateHandlers = [];
        let graphDiff = freshGraphDiff();

        graph.onCommit = function(handler) {
            updateHandlers.push(handler);
        };
        graph.unsubscribeOnCommit = function(handler) {
            _.remove(updateHandlers, handler);
        };
        graph.commitInternal = function() {
            refreshIndex(this);
            _.each(updateHandlers, handler => handler(graphDiff));
            graphDiff = freshGraphDiff();
        };
        // TODO: having sets instead of arrays would be better...nodes,relations,inrelations,outrelations
        graph.addNodeInternal = function(node) {
            defineNodeProperties(node);
            this.nodes.push(node);
            graphDiff.newNodes.push(node);
            graphDiff.removedNodes.remove(node.id);
        };
        graph.addRelationInternal = function(relation) {
            this.edges.push(relation);
            graphDiff.newRelations.push(relation);
            graphDiff.removedRelations.remove(relation);
        };
        graph.removeNodeInternal = function(node) {
            this.nodes.remove(node);
            graphDiff.newNodes.remove(node);
            graphDiff.removedNodes.push(node);

            _(this.relations).select(r => r.source === node || r.target === node).each(r => this.removeRelation(r)).value();
        };
        graph.removeRelationInternal = function(relation) {
            this.relations.remove(relation);
            graphDiff.newRelations.remove(relation);
            graphDiff.removedRelations.push(relation);

            relation.target.inRelations.remove(relation);
            relation.source.outRelations.remove(relation);
        };
        graph.rootNode = _.find(graph.nodes, {
            id: graph.$pk
        });

        function freshGraphDiff() {
            return {
                newRelations: UniqArr.relation(),
                newNodes: UniqArr.node(),
                removedRelations: UniqArr.relation(),
                removedNodes: UniqArr.node()
            };
        }
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

        this.$encode = self.$encode;
    }
}
