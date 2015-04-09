angular.module("wust").filter("fuzzyFilter", ["$filter",
    function($filter) {
        return (data, obj) => {
            _.forIn(obj, (value, key) => {
                if (!_.isString(value) && !_.isNumber(value))
                    return;

                _.each(value.split(" "), (val) => {
                    data = $filter("filter")(data, {
                        [key]: val || ""
                    });
                });
            });

            return data;
        };
    }
]);
