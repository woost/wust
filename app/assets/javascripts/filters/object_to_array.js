app.filter('objectToArray', function() {
    return function(input) {
        var out = [];
        input.forEach(function(i) {
            out.push(i);
        });
        return out;
    };
});
