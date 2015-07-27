angular.module("wust.services").provider("DiscourseNode", DiscourseNode);

DiscourseNode.$inject = [];

function DiscourseNode() {
    let discourseMap = {};
    this.setLabel = _.wrap("label", set);
    this.setCss = _.wrap("css", set);
    this.setState = _.wrap("state", set);
    this.$get = get;

    get.$inject = ["$state", "$injector"];
    function get($state, $injector) {
        _.mapValues(discourseMap, node => _.merge(node, {
            //TODO: we sometimes use ng-href instead of ui-sref because the info seems to be
            //filled too late for ui-router to recognize the state
            getHref: id => node.state && (id !== undefined) ? $state.href(node.state, { id }) : "#",
            // check wether a state is defined. If it isn't stay on the current page.
            getState: id => node.state && (id !== undefined) ? `${node.state}({id: "${id}"})` : ".",
            gotoState: id => { if (node.state && (id !== undefined)) $state.go(node.state, {id: id}); },
            service: $injector.get(node.name),
            tagCss: tag => tag.isType ? `tag_${tag.title}` : `tag__none`
        }));

        let mappings = _(_.values(discourseMap)).map(node => {
            return {
                [node.label]: node
            };
        }).reduce(_.merge);

        let defaultNode = {
            css: "hyperrelation",
            tagCss: _.constant(""),
            getHref: _.constant("#"),
            getState: _.constant("."),
            gotoState: _.noop
        };

        return _.merge(discourseMap, {
            get: label => mappings[label] || defaultNode
        });
    }

    function set(property, name, value) {
        discourseMap[name] = discourseMap[name] || { name: name };

        discourseMap[name][property] = value;
        return value;
    }
}
