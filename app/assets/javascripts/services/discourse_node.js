angular.module("wust").service("DiscourseNode", function() {
    this.goal = {
        css: "discourse_goal",
        state: "goals",
        color: "#6DFFB4",
        label: "GOAL"
    };

    this.problem = {
        css: "discourse_problem",
        state: "problems",
        color: "#FFDC6D",
        label: "PROBLEM"
    };

    this.idea = {
        css: "discourse_idea",
        state: "ideas",
        color: "#6DB5FF",
        label: "IDEA"
    };

    var mappings = _([this.goal, this.problem, this.idea]).map(node => {
        return {
            [node.label]: node
        };
    }).reduce(_.merge);

    this.get = _.propertyOf(mappings);
});
