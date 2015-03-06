app.service('Problem', function($resource) {
    var problemService = $resource('/api/problems/:id', { id: '@id' });
    var ideaService = $resource('/api/problems/:id/ideas', { id: '@id' });
    var goalService = $resource('/api/problems/:id/goals', { id: '@id' });

    this.get = get;
    this.query = query(problemService);
    this.queryIdeas = query(ideaService);
    this.queryGoals = query(goalService);
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
