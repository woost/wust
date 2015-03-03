app.filter('objectToArray', function() {
    return function(input) {
        var out = [];
        angular.forEach(input, function(i) {
            out.push(i);
        });
        return out;
    };
});
