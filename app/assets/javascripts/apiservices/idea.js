app.service('Idea', function($resource, Node) {
    var service = $resource('/api/v1/ideas/:id', { id: '@id' });
    var goalService = $resource('/api/v1/ideas/:id/goals', { id: '@id' });
    var problemService = $resource('/api/v1/ideas/:id/problems', { id: '@id' });

    this.get = Node.get(service);
    this.create = Node.create(service);
    this.query = Node.query(service);
    this.queryGoals = Node.query(goalService);
    this.queryProblems = Node.query(problemService);
    this.createGoal = Node.createConnected(goalService);
    this.createProblem = Node.createConnected(problemService);
});
