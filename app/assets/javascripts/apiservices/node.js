angular.module("wust").factory("Node", function($resource) {
    return function(name) {
        var prefix = `/api/v1/${name}`;

        var service = $resource(`${prefix}/:id`, {
            id: "@id"
        }, {
            update: {
                method: "PUT"
            }
        });

        _.assign(this, getResourceFunctions(service, {
            get: get,
            create: create,
            remove: remove,
            update: update,
            query: query
        }));

        this.goals = getConnectedFunctions("goals");
        this.problems = getConnectedFunctions("problems");
        this.ideas = getConnectedFunctions("ideas");

        function createConnectedResource(resource) {
            return $resource(`${prefix}/:id/${resource}/:otherId`, {
                id: "@id",
                otherId: "@otherId"
            });
        }

        function getConnectedFunctions(name) {
            return getResourceFunctions(createConnectedResource(name), {
                create: createConnected,
                remove: removeConnected,
                query: query
            });
        }

        function getResourceFunctions(service, functions) {
            return _.mapValues(functions, f => _.wrap(service, f));
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

        function update(service, id, obj) {
            return service.update({
                id: id
            }, _.pick(obj, "title"));
        }

        function remove(service, id) {
            return service.remove({
                id: id
            });
        }

        function create(service, obj) {
            return service.save(_.pick(obj, "title"));
        }
    };
});
