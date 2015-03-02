app.service('Graph', function($resource) {
    var service = $resource('/api/graphs/:id', {id: '@id'});
    this.get = get;
    this.create = create;

    function get(id) {
        return service.get({id: id});
    }

    function create(obj) {
        return service.save(obj);
    }
});
