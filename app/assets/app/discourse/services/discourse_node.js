angular.module("wust.discourse").provider("DiscourseNode", function() {
    let discourseMap = {};
    this.setLabel = _.wrap("label", set);
    this.setCss = _.wrap("css", set);
    this.setState = _.wrap("state", set);
    this.$get = get;

    function get() {
        let mappings = _(_.values(discourseMap)).map(node => {
            return {
                [node.label]: node
            };
        }).reduce(_.merge);

        return _.merge(discourseMap, {
            get: _.propertyOf(mappings)
        });
    }

    function set(property, name, value) {
        discourseMap[name] = discourseMap[name] || {};

        discourseMap[name][property] = value;
        return value;
    }
});
