app.service('Goal', function($resource, Node) {
    var service = $resource('/api/goals/:id', { id: '@id' });
    var ideaService = $resource('/api/goals/:id/ideas', { id: '@id' });
    var problemService = $resource('/api/goals/:id/problems', { id: '@id' });

    this.get = Node.get(service);
    this.queryIdeas = Node.query(ideaService);
    this.queryProblems = Node.query(problemService);
    this.createIdea = Node.createConnected(ideaService);
    this.createProblem = Node.createConnected(problemService);
});
