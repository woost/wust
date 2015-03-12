app.service('Goal', function($resource, Node) {
    var service = $resource('/api/v1/goals/:id', { id: '@id' });
    var ideaService = $resource('/api/v1/goals/:id/ideas/:otherId', { id: '@id', otherId: '@otherId'});
    var problemService = $resource('/api/v1/goals/:id/problems/:otherId', { id: '@id', otherId: '@otherId' });

    this.get = Node.get(service);
    this.create = Node.create(service);
    this.remove = Node.remove(service);
    this.query = Node.query(service);
    this.queryIdeas = Node.query(ideaService);
    this.queryProblems = Node.query(problemService);
    this.createIdea = Node.createConnected(ideaService);
    this.createProblem = Node.createConnected(problemService);
    this.removeProblem = Node.removeConnected(problemService);
});
