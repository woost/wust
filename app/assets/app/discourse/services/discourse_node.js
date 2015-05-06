angular.module("wust.discourse").provider("DiscourseNode", DiscourseNode);

DiscourseNode.$inject = [];

function DiscourseNode() {
    let discourseMap = {};
    this.setLabel = _.wrap("label", set);
    this.setCss = _.wrap("css", set);
    this.setState = _.wrap("state", set);
    this.$get = get;

    get.$inject = ["$state"];
    function get($state) {
        _.mapValues(discourseMap, node => _.merge(node, {
            getState: id => node.state ? `${node.state}({id: "${id}"})` : ".",
            gotoState: id => { if (node.state) $state.go(node.state, {id: id}); }
        }));
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
}
