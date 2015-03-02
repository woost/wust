app.service('Graph', function($resource) {
    var service = $resource('/api/graphs/:id', {
        id: '@id'
    });
    this.get = get;
    this.create = create;
    this.remove = remove;

    function get(id) {
        return service.get({
            id: id
        });
    }

    function remove(id) {
        return service.remove({
            id: id
        });
    }

    function create(obj) {
        return service.save(obj);
    }
});
