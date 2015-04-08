angular.module("wust").filter("fuzzyFilter", ["$filter",
    function($filter) {
        return (data, obj) => {
            _.forIn(obj, (value, key) => {
                _.each(value.split(" "), (val) => {
                    var search = {};
                    search[key] = val || "";
                    data = $filter("filter")(data, search);
                });
            });

            return data;
        };
    }
]);
