app.factory('Node', function($resource) {
    return function(name) {
        var prefix = '/api/v1/' + name;

        var service = $resource(prefix + '/:id', {
            id: '@id'
        });

        var goalService = createResource("goals");
        var ideaService = createResource("ideas");
        var problemService = createResource("problems");

        this.get = _.wrap(service, get);
        this.create = _.wrap(service, create);
        this.remove = _.wrap(service, remove);
        this.query = _.wrap(service, query);

        this.goals = getCallbackObject(goalService);
        this.problems = getCallbackObject(problemService);
        this.ideas = getCallbackObject(ideaService);

        function createResource(resource) {
            return $resource(prefix + '/:id/' + resource + '/:otherId', {
                id: '@id',
                otherId: '@otherId'
            });
        }

        function getCallbackObject(service) {
            return {
                query: _.wrap(service, query),
                create: _.wrap(service, createConnected),
                remove: _.wrap(service, removeConnected)
            };
        }

        function createConnected(service, id, otherId) {
            return service.save({
                id: id,
                otherId: otherId
            });
        }

        function removeConnected(service, id, otherId) {
            return service.remove({
                id: id,
                otherId: otherId
            });
        }

        function query(service, id) {
            return service.query({
                id: id
            });
        }

        function get(service, id) {
            return service.get({
                id: id
            });
        }

        function remove(service, id) {
            return service.remove({
                id: id
            });
        }

        function create(service, obj) {
            return service.save(obj);
        }
    };
});
