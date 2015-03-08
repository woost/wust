app.service('Goal', function($resource) {
    var service = $resource('/api/goals/:id', { id: '@id' });
    this.get = get;

    function get(id) {
        return service.get({ id: id });
    }
});
