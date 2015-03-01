app.service('Graph', function($resource) {
    var service = $resource('/api/graphs/:id', {id: '@id'});
    this.get = get;

    function get(id) {
        return service.get({id: id});
    }
});
