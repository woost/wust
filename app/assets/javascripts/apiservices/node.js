app.factory('Node', function($resource) {
    return function(name) {
        var prefix = '/api/v1/' + name;
        var service = $resource(prefix + '/:id', {
            id: '@id'
        });
        var goalService = $resource(prefix + '/:id/goals/:otherId', {
            id: '@id',
            otherId: '@otherId'
        });
        var ideaService = $resource(prefix + '/:id/ideas/:otherId', {
            id: '@id',
            otherId: '@otherId'
        });
        var problemService = $resource(prefix + '/:id/problems/:otherId', {
            id: '@id',
            otherId: '@otherId'
        });

        this.get = get(service);
        this.create = create(service);
        this.remove = remove(service);
        this.query = query(service);

        this.goals = getCallbackObject(goalService);
        this.problems = getCallbackObject(problemService);
        this.ideas = getCallbackObject(ideaService);

        function getCallbackObject(service) {
            return {
                query: query(service),
                create: createConnected(service),
                remove: removeConnected(service)
            };
        }

        function createConnected(service) {
            return function(id, otherId) {
                return service.save({
                    id: id,
                    otherId: otherId
                });
            };
        }

        function removeConnected(service) {
            return function(id, otherId) {
                return service.remove({
                    id: id,
                    otherId: otherId
                });
            };
        }

        function query(service) {
            return function(id) {
                return service.query({
                    id: id
                });
            };
        }

        function get(service) {
            return function(id) {
                return service.get({
                    id: id
                });
            };
        }

        function remove(service) {
            return function(id) {
                return service.remove({
                    id: id
                });
            };
        }

        function create(service) {
            return function(obj) {
                return service.save(obj);
            };
        }
    };
});
