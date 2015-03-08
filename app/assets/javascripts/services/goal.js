app.service('Goal', function($resource, Node) {
    var service = $resource('/api/v1/goals/:id', { id: '@id' });
    var ideaService = $resource('/api/v1/goals/:id/ideas', { id: '@id' });
    var problemService = $resource('/api/v1/goals/:id/problems', { id: '@id' });

    this.get = Node.get(service);
    this.queryIdeas = Node.query(ideaService);
    this.queryProblems = Node.query(problemService);
    this.createIdea = Node.createConnected(ideaService);
    this.createProblem = Node.createConnected(problemService);
});
