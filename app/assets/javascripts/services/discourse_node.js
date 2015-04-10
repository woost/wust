angular.module("wust").service("DiscourseNode", function() {
    this.goal = {
        css: "discourse_goal",
        state: "goals",
        label: "GOAL"
    };

    this.problem = {
        css: "discourse_problem",
        state: "problems",
        label: "PROBLEM"
    };

    this.idea = {
        css: "discourse_idea",
        state: "ideas",
        label: "IDEA"
    };

    var mappings = _([this.goal, this.problem, this.idea]).map(node => {
        return {
            [node.label]: node
        };
    }).reduce(_.merge);

    this.get = _.propertyOf(mappings);
});
