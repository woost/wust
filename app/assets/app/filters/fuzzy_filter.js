angular.module("wust.filters").filter("fuzzyFilter", fuzzyFilter);

fuzzyFilter.$inject = ["$filter"];

function fuzzyFilter($filter) {
    return (data, obj) => {
        _.forIn(obj, (value, key) => {
            if (!_.isString(value) && !_.isNumber(value))
                return;

            _.each(value.split(" "), val => {
                data = $filter("filter")(data, {
                    [key]: val || ""
                });
            });
        });

        return data;
    };
}
