angular.module("wust").service('Search', function($resource) {
    var prefix = '/api/v1/';

    var service = $resource('/api/v1/search/:term/:type', {
        term: "@term",
        type: "@type"
    });

    this.query = _.wrap(undefined, query);
    this.queryGoals = _.wrap("goals", query);
    this.queryProblems = _.wrap("problems", query);
    this.queryIdeas = _.wrap("ideas", query);

    function query(type, term) {
        return service.query({
            term: term,
            type: type
        });
    }
});
