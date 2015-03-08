app.service('Idea', function($resource, Node) {
    var service = $resource('/api/ideas/:id', { id: '@id' });
    var goalService = $resource('/api/ideas/:id/goals', { id: '@id' });
    var problemService = $resource('/api/ideas/:id/problems', { id: '@id' });

    this.get = Node.get(service);
    this.queryGoals = Node.query(goalService);
    this.queryProblems = Node.query(problemService);
    this.createGoal = Node.createConnected(goalService);
    this.createProblem = Node.createConnected(problemService);
});
