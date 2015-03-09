app.service('Node', function() {
    this.createConnected = createConnected;
    this.query = query;
    this.get = get;
    this.create = create;

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

    function get(service) {
        return function(id) {
            return service.get({ id: id });
        };
    }

    function create(service) {
        return function(obj) {
            return service.save(obj);
        };
    }
});
