app.service('Problem', function($resource) {
    var service = $resource('/api/problems/:id', { id: '@id' });
    var ideaService = $resource('/api/problems/:id/ideas', { id: '@id' });
    var goalService = $resource('/api/problems/:id/goals', { id: '@id' });
    var problemService = $resource('/api/problems/:id/problems', { id: '@id' });

    this.get = get;
    this.create = create;
    this.query = query(service);
    this.queryIdeas = query(ideaService);
    this.queryGoals = query(goalService);
    this.queryProblems = query(problemService);
    this.createIdea = createConnected(ideaService);
    this.createGoal = createConnected(goalService);
    this.createProblem = createConnected(problemService);

    function createConnected(service) {
        return function(id, obj) {
            return service.save({id: id}, obj);
        };
    }

    function query(service) {
        return function(id) {
            return service.query({ id: id });
        };
    }

    function get(id) {
        return service.get({ id: id });
    }

    function create(obj) {
        return service.save(obj);
    }
});
