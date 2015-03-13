app.filter('fuzzyFilter', ['$filter',
    function($filter) {
        return function(data, obj) {
            _.forIn(obj, function(value, key) {
                _.each(value.split(' '), function(val) {
                    var search = {};
                    search[key] = val || "";
                    data = $filter('filter')(data, search);
                });
            });

            return data;
        };
    }
]);
