app.service('Search', function($resource) {
    var service = $resource('/api/v1/search/:term/:label', {
        term: "@term",
        label: "@label"
    });

    this.query = query();
    this.queryIdeas = query("IDEA");
    this.queryGoals = query("GOAL");
    this.queryProblems = query("PROBLEM");

    function query(label) {
        return function(term) {
            return service.query({
                term: term,
                label: label
            });
        };
    }
});
