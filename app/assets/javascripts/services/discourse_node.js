app.service('DiscourseNode', function() {
    this.goal = {
        css: "discourse_goal",
        state: "goals"
    };

    this.problem = {
        css: "discourse_problem",
        state: "problems"
    };

    this.idea = {
        css: "discourse_idea",
        state: "ideas"
    };

    this.getState = getProperty("state");
    this.getCss = getProperty("css");

    var mappings = {
        GOAL: this.goal,
        PROBLEM: this.problem,
        IDEA: this.idea,
    };

    function getProperty(property) {
        return function(label) {
            return mappings[label][property];
        };
    }
});
