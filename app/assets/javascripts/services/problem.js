app.service('Problem', function($resource) {
    var problemService = $resource('/api/problems/:id', { id: '@id' }, {
        query: { method: 'get', isArray: true }
    });
    var ideaService = $resource('/api/problems/:id/ideas', { id: '@id' }, {
        query: { method: 'get', isArray: true }
    });
    var goalService = $resource('/api/problems/:id/goals', { id: '@id' }, {
        query: { method: 'get', isArray: true }
    });

    this.get = get;
    this.query = query(problemService);
    this.queryIdeas = get(ideaService);
    this.queryGoals = get(goalService);
    this.create = create;

    function query(service) {
        return function(id) {
            return service.query({ id: id });
        };
    }

    function get(id) {
        return problemService.get({ id: id });
    }

    function create(obj) {
        return problemService.save(obj);
    }
});
