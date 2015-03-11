app.service('Node', function() {
    this.query = query;
    this.get = get;
    this.create = create;
    this.remove = remove;
    this.createConnected = createConnected;
    this.removeConnected = removeConnected;

    function createConnected(service) {
        return function(id, obj) {
            return service.save({id: id}, obj);
        };
    }

    function removeConnected(service) {
        return function(id, otherId) {
            return service.remove({id: id, otherId: otherId});
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

    function remove(service) {
        return function(id) {
            return service.remove({ id: id });
        };
    }

    function create(service) {
        return function(obj) {
            return service.save(obj);
        };
    }
});
