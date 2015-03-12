app.service('Problem', function($resource, Node) {
    var service = $resource('/api/v1/problems/:id', { id: '@id' });
    var ideaService = $resource('/api/v1/problems/:id/ideas/:otherId', { id: '@id', otherId: '@otherId' });
    var goalService = $resource('/api/v1/problems/:id/goals/:otherId', { id: '@id', otherId: '@otherId' });
    var problemService = $resource('/api/v1/problems/:id/problems/:otherId', { id: '@id', otherId: '@otherId' });

    this.get = Node.get(service);
    this.create = Node.create(service);
    this.remove = Node.remove(service);
    this.query = Node.query(service);
    this.queryIdeas = Node.query(ideaService);
    this.queryGoals = Node.query(goalService);
    this.queryProblems = Node.query(problemService);
    this.createIdea = Node.createConnected(ideaService);
    this.createGoal = Node.createConnected(goalService);
    this.createProblem = Node.createConnected(problemService);
    this.removeGoal = Node.removeConnected(goalService);
    this.removeProblem = Node.removeConnected(problemService);
});
