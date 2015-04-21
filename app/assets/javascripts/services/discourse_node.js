angular.module("wust").service("DiscourseNode", function(SchemaInfo) {
    this.goal = _.merge({
        css: "discourse_goal",
    }, SchemaInfo.Goal);

    this.problem = _.merge({
        css: "discourse_problem",
    }, SchemaInfo.Problem);

    this.idea = _.merge({
        css: "discourse_idea",
    }, SchemaInfo.Idea);

    let mappings = _([this.goal, this.problem, this.idea]).map(node => {
        return {
            [node.label]: node
        };
    }).reduce(_.merge);

    this.get = _.propertyOf(mappings);
});
