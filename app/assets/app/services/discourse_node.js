angular.module("wust.services").provider("DiscourseNode", DiscourseNode);

DiscourseNode.$inject = [];

function DiscourseNode() {
    let discourseMap = {};
    this.setLabel = _.wrap("label", set);
    this.$get = get;

    get.$inject = ["$state", "$injector"];
    function get($state, $injector) {
        _.mapValues(discourseMap, node => _.merge(node, {
            service: $injector.get(node.name)
        }));

        let mappings = _(_.values(discourseMap)).map(node => {
            return {
                [node.label]: node
            };
        }).reduce(_.merge);

        return _.merge(discourseMap, {
            get: label => mappings[label] || {}
        });
    }

    function set(property, name, value) {
        discourseMap[name] = discourseMap[name] || { name: name };

        discourseMap[name][property] = value;
        return value;
    }
}
