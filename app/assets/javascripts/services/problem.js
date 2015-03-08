app.service('Problem', function($resource, Node) {
    var service = $resource('/api/problems/:id', { id: '@id' });
    var ideaService = $resource('/api/problems/:id/ideas', { id: '@id' });
    var goalService = $resource('/api/problems/:id/goals', { id: '@id' });
    var problemService = $resource('/api/problems/:id/problems', { id: '@id' });

    this.get = Node.get(service);
    this.create = Node.create(service);
    this.query = Node.query(service);
    this.queryIdeas = Node.query(ideaService);
    this.queryGoals = Node.query(goalService);
    this.queryProblems = Node.query(problemService);
    this.createIdea = Node.createConnected(ideaService);
    this.createGoal = Node.createConnected(goalService);
    this.createProblem = Node.createConnected(problemService);
});
