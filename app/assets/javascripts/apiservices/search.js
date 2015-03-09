app.factory('Search', function($resource) {
    var service = $resource('/api/v1/search/:term', {
        term: "@term"
    });
    return function(term) {
        return service.query({
            term: term
        });
    };
});
