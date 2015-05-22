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
        let stateHelper = (node) => {
            return {
                // check wether a state is defined. If it isn't stay on the current page.
                getState: id => node.state ? `${node.state}({id: "${id}"})` : ".",
                gotoState: id => { if (node.state) $state.go(node.state, {id: id}); }
            };
        };
        _.mapValues(discourseMap, node => _.merge(node, stateHelper(node)));
        let mappings = _(_.values(discourseMap)).map(node => {
            return {
                [node.label]: node
            };
        }).reduce(_.merge);

        let defaultNode = {
            css: "relation_label"
        };

        let defaultNodeWithState = _.merge(defaultNode, stateHelper(defaultNode));

        return _.merge(discourseMap, {
            get: (label) => mappings[label] || defaultNodeWithState
        });
    }

    function set(property, name, value) {
        discourseMap[name] = discourseMap[name] || {};

        discourseMap[name][property] = value;
        return value;
    }
}
