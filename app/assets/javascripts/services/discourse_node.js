angular.module("wust").service("DiscourseNode", function(Labels) {
    this.goal = {
        css: "discourse_goal",
        state: "goals",
        label: Labels.Goal
    };

    this.problem = {
        css: "discourse_problem",
        state: "problems",
        label: Labels.Problem
    };

    this.idea = {
        css: "discourse_idea",
        state: "ideas",
        label: Labels.Idea
    };

    let mappings = _([this.goal, this.problem, this.idea]).map(node => {
        return {
            [node.label]: node
        };
    }).reduce(_.merge);

    this.get = _.propertyOf(mappings);
});
