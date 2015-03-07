app.service('Idea', function($resource) {
    var service = $resource('/api/ideas/:id', { id: '@id' });
    this.get = get;

    function get(id) {
        return service.get({ id: id });
    }
});
